package no.nav.dagpenger.mottak.observers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingObserver.Type.Ettersending
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysClient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysOppgaveRequest
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_ORDINÆR_ETTERSENDING
import no.nav.dagpenger.mottak.tjenester.SaksbehandlingKlient
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FerdigstiltEttersendingObserverTest {
    @Test
    fun `Hvis varsle om ettersending kall gosys`() {
        val identVarsle = "skalVarsle"
        val identIkkeVarsle = "skalIkkeVarsle"
        val saksbehandlingKlient =
            mockk<SaksbehandlingKlient>().also {
                coEvery { it.skalVarsleOmEttersending(søknadId = any(), ident = identVarsle) } returns true
                coEvery { it.skalVarsleOmEttersending(søknadId = any(), ident = identIkkeVarsle) } returns false
            }
        val slot = CapturingSlot<GosysOppgaveRequest>()
        val gosysClient =
            mockk<GosysClient>().also {
                coEvery {
                    it.opprettOppgave(capture(slot))
                } returns "oppgaveId"
            }

        val observer = FerdigstiltEttersendingObserver(saksbehandlingKlient, gosysClient)

        observer.innsendingFerdigstilt(
            InnsendingObserver.InnsendingEvent(
                type = Ettersending,
                skjemaKode = DAGPENGESØKNAD_ORDINÆR_ETTERSENDING.skjemakode,
                journalpostId = "journalpostId",
                aktørId = "aktørId",
                fødselsnummer = identVarsle,
                fagsakId = "fagsakId",
                datoRegistrert = LocalDateTime.now(),
                søknadsData = jacksonObjectMapper().readTree("""{"søknad_uuid": "søknad_uuid"}"""),
                behandlendeEnhet = "tildeleEnhetsnr",
                tittel = "tittel",
            ),
        )

        coVerify(exactly = 1) {
            gosysClient.opprettOppgave(any())
        }
        slot.captured.let {
            it.journalpostId shouldBe "journalpostId"
            it.aktoerId shouldBe "aktørId"
            it.tildeltEnhetsnr shouldBe "tildeleEnhetsnr"
            it.beskrivelse shouldBe "Ettersending til dagpengesøknad i ny løsning"
        }
    }
}
