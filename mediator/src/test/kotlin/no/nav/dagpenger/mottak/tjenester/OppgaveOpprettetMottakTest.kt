package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveFeilet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.OppgaveOpprettet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OppgaveOpprettetMottakTest {
    private val testRapid = TestRapid()
    private val innsendingMediatorMock = mockk<InnsendingMediator>(relaxed = true)

    init {
        OppgaveOpprettetMottak(
            innsendingMediator = innsendingMediatorMock,
            rapidsConnection = testRapid,
        )
    }

    @Test
    fun `Skal motta hendelse om opprettet oppgave i Dagpenger`() {
        testRapid.sendTestMessage(opprettDagpengerOppgaveHendelse)

        verify(exactly = 1) {
            innsendingMediatorMock.håndter(any() as OppgaveOpprettet)
        }
    }

    @Test
    fun `Skal motta hendelse om opprettet oppgave i Arena`() {
        testRapid.sendTestMessage(opprettArenaOppgaveHendelse)

        verify(exactly = 1) {
            innsendingMediatorMock.håndter(any() as ArenaOppgaveOpprettet)
        }
    }

    @Test
    fun `Skal motta hendelse om feil ved oppretting av oppgave i Arena`() {
        testRapid.sendTestMessage(opprettArenaOppgaveHendelseFeilet)

        verify(exactly = 1) {
            innsendingMediatorMock.håndter(any() as ArenaOppgaveFeilet)
        }
    }

    //language=JSON
    private val opprettDagpengerOppgaveHendelse: String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "${UUID.randomUUID()}",
          "@løsning": {
            "OpprettOppgave": {
                "fagsakId": "${UUID.randomUUID()}",
                "oppgaveId": "${UUID.randomUUID()}",
                "fagsystem": "DAGPENGER"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private val opprettArenaOppgaveHendelseFeilet: String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "${UUID.randomUUID()}",
          "@løsning": {
            "OpprettOppgave": {
              "@feil": "Kunne ikke opprette arena oppgave",
              "fagsystem": "ARENA"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private val opprettArenaOppgaveHendelse: String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "${UUID.randomUUID()}",
          "@løsning": {
            "OpprettOppgave": {
                "fagsakId": "12345",
                "oppgaveId": "12345678",
                "fagsystem": "ARENA"
            }
          }
        }
        """.trimIndent()
}
