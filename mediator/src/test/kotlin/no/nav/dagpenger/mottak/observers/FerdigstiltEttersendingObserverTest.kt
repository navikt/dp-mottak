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
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource
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
    fun `Hvis varsle om ettersending med oppgave i gosys`() {
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
                    ident = identVarsle,
                    innsendingType = Ettersending,
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
    fun `Hvis ikke varsle om ettersending med oppgave i gosys`() {
        val gosysClient = mockk<GosysClient>()

        val observer = FerdigstiltEttersendingObserver(saksbehandlingKlient, gosysClient)
        observer.innsendingFerdigstilt(
            innsendingFerdigstiltEvent(
                ident = identIkkeVarsle,
                innsendingType = Ettersending,
            ),
        )
        coVerify(exactly = 0) { gosysClient.opprettOppgave(any()) }
    }

    @ParameterizedTest
    @EnumSource(InnsendingObserver.Type::class)
    fun `Skal bare sjekke behov for varsling for ettersendinger hvis skjemakode er ettersending til dagpengesøknad`(
        innsendingType: InnsendingObserver.Type,
    ) {
        val mockGosysClient =
            mockk<GosysClient>().also {
                coEvery {
                    it.opprettOppgave(any())
                } returns "oppgaveId"
            }

        val observer = FerdigstiltEttersendingObserver(saksbehandlingKlient, mockGosysClient)
        observer.innsendingFerdigstilt(
            innsendingFerdigstiltEvent(
                ident = identVarsle,
                innsendingType = innsendingType,
            ),
        )
        if (innsendingType == Ettersending) {
            coVerify(exactly = 1) { saksbehandlingKlient.skalVarsleOmEttersending(any(), any()) }
        } else {
            coVerify(exactly = 0) { saksbehandlingKlient.skalVarsleOmEttersending(any(), any()) }
        }
    }

    private fun innsendingFerdigstiltEvent(
        ident: String,
        innsendingType: InnsendingObserver.Type,
    ): InnsendingObserver.InnsendingEvent {
        return InnsendingObserver.InnsendingEvent(
            type = innsendingType,
            skjemaKode = "kod",
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
