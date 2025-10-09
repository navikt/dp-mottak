package no.nav.dagpenger.mottak.observers

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import io.mockk.CapturingSlot
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingObserver.Type.Ettersending
import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingKlient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysClient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysOppgaveRequest
import no.nav.dagpenger.mottak.meldinger.SkjemaType
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_ORDINÆR_ETTERSENDING
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class FerdigstiltEttersendingObserverTest {
    private val identVarsle = "skalVarsle"
    private val identIkkeVarsle = "skalIkkeVarsle"
    private val saksbehandlingKlient =
        mockk<SaksbehandlingKlient>().also {
            coEvery { it.skalVarsleOmEttersending(søknadId = any(), ident = identVarsle) } returns true
            coEvery { it.skalVarsleOmEttersending(søknadId = any(), ident = identIkkeVarsle) } returns false
        }

    @Test
    fun `Hvis varsle om ettersending kall gosys`() {
        val slot = CapturingSlot<GosysOppgaveRequest>()
        val gosysClient =
            mockk<GosysClient>().also {
                coEvery {
                    it.opprettOppgave(capture(slot))
                } returns "oppgaveId"
            }

        val observer = FerdigstiltEttersendingObserver(saksbehandlingKlient, gosysClient)

        observer.innsendingFerdigstilt(
            event =
                innsendingFerdigstiltEvent(
                    identVarsle,
                    DAGPENGESØKNAD_ORDINÆR_ETTERSENDING.skjemakode,
                ),
        )

        coVerify(exactly = 1) {
            gosysClient.opprettOppgave(any())
        }
        slot.captured.let {
            it.journalpostId shouldBe "journalpostId"
            it.aktoerId shouldBe "aktørId"
            it.tildeltEnhetsnr shouldBe "tildeleEnhetsnr"
            it.beskrivelse shouldBe "Ettersendelse til dagpengesøknad i ny løsning"
            it.oppgavetype shouldBe "ETTERSEND_MOTT"
        }
    }

    @Test
    fun `Hvis ikke varsle om ettersending ikke kall gosys`() {
        val gosysClient = mockk<GosysClient>()

        val observer = FerdigstiltEttersendingObserver(saksbehandlingKlient, gosysClient)
        observer.innsendingFerdigstilt(
            innsendingFerdigstiltEvent(
                ident = identIkkeVarsle,
                skjemaKode = DAGPENGESØKNAD_ORDINÆR_ETTERSENDING.skjemakode,
            ),
        )
        coVerify(exactly = 0) { gosysClient.opprettOppgave(any()) }
    }

    @Test
    fun `Hvis ikke varsle om ettersending hvis skjemakode ikke er ettersending til søknad`() {
        val mockSaksbehandlingKlient = mockk<SaksbehandlingKlient>()

        val observer = FerdigstiltEttersendingObserver(mockSaksbehandlingKlient, mockk<GosysClient>())
        observer.innsendingFerdigstilt(
            innsendingFerdigstiltEvent(
                ident = identVarsle,
                skjemaKode = SkjemaType.DAGPENGESØKNAD_ETABLERING.skjemakode,
            ),
        )
        coVerify(exactly = 0) { mockSaksbehandlingKlient.skalVarsleOmEttersending(any(), any()) }
    }

    private fun innsendingFerdigstiltEvent(
        ident: String,
        skjemaKode: String,
    ): InnsendingObserver.InnsendingEvent {
        return InnsendingObserver.InnsendingEvent(
            type = Ettersending,
            skjemaKode = skjemaKode,
            journalpostId = "journalpostId",
            aktørId = "aktørId",
            fødselsnummer = ident,
            fagsakId = "fagsakId",
            oppgaveId = "oppgaveId",
            datoRegistrert = LocalDateTime.now(),
            søknadsData = jacksonObjectMapper().readTree("""{"søknad_uuid": "søknad_uuid"}"""),
            behandlendeEnhet = "tildeleEnhetsnr",
            tittel = "tittel",
        )
    }
}
