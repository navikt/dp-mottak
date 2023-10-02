package no.nav.dagpenger.mottak.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveFeilet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime
import java.util.UUID

internal class OpprettArenaOppgaveMottakTest {
    private val testRapid = TestRapid()
    private val innsendingMediator = mockk<InnsendingMediator>(relaxed = true)

    init {
        OpprettArenaOppgaveMottak(innsendingMediator, testRapid)
    }

    @ParameterizedTest
    @ValueSource(strings = ["OpprettStartVedtakOppgave", "OpprettVurderhenvendelseOppgave"])
    fun `Skal håndtere at arena oppgave opprettet `(behov: String) {
        testRapid.sendTestMessage(opprettArenaOppgaveHendelse(behov))
        verify(exactly = 1) {
            innsendingMediator.håndter(any() as ArenaOppgaveOpprettet)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["OpprettStartVedtakOppgave", "OpprettVurderhenvendelseOppgave"])
    fun `Skal håndtere at arena oppgave opprettelse feilet `(behov: String) {
        testRapid.sendTestMessage(opprettArenaOppgaveHendelseFeilet(behov))
        verify(exactly = 1) {
            innsendingMediator.håndter(any() as ArenaOppgaveFeilet)
        }
    }

    //language=JSON
    private fun opprettArenaOppgaveHendelseFeilet(behov: String): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "$behov"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "${UUID.randomUUID()}",
          "@løsning": {
            "$behov": {
              "@feil": "Kunne ikke opprette arena oppgave"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun opprettArenaOppgaveHendelse(behov: String): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "$behov"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "${UUID.randomUUID()}",
          "@løsning": {
            "$behov": {
                "fagsakId": "12345",
                "oppgaveId": "12345678"
            }
          }
        }
        """.trimIndent()
}
