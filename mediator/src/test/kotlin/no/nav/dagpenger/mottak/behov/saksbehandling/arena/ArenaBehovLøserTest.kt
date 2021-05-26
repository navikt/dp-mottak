package no.nav.dagpenger.mottak.behov.saksbehandling.arena

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

private const val JOURNALPOST_ID = "2345678"

internal class ArenaBehovLøserTest {

    private val testRapid = TestRapid()

    init {
        ArenaBehovLøser(
            arenaOppslag = object : ArenaOppslag {
                override suspend fun harEksisterendeSaker(fnr: String): Boolean = true
                override suspend fun opprettStartVedtakOppgave(
                    journalpostId: String,
                    parametere: OpprettArenaOppgaveParametere
                ) = OpprettVedtakOppgaveResponse(
                    fagsakId = "123",
                    oppgaveId = "123"
                )
                override suspend fun opprettVurderHenvendelsOppgave(
                    journalpostId: String,
                    parametere: OpprettArenaOppgaveParametere
                ) = OpprettVedtakOppgaveResponse(
                    fagsakId = null,
                    oppgaveId = "123"
                )
            },
            rapidsConnection = testRapid
        )
    }

    @BeforeEach
    fun `clear rapid`() {
        testRapid.reset()
    }

    @Test
    fun `Løser eksisterende saker behov`() {
        testRapid.sendTestMessage(eksisterendeSakerBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertDoesNotThrow { field(0, "@løsning") }
            assertTrue(field(0, "@løsning")["EksisterendeSaker"]["harEksisterendeSak"].asBoolean())
        }
    }

    @Test
    fun `Løser OpprettStartVedtakOppgave behov`() {
        testRapid.sendTestMessage(opprettStartVedtakBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertDoesNotThrow { field(0, "@løsning") }
            assertEquals("123", field(0, "@løsning")["OpprettStartVedtakOppgave"]["fagsakId"].asText())
            assertEquals("123", field(0, "@løsning")["OpprettStartVedtakOppgave"]["oppgaveId"].asText())
            assertEquals(JOURNALPOST_ID, field(0, "@løsning")["OpprettStartVedtakOppgave"]["journalpostId"].asText())
        }
    }

    @Test
    fun `Løser 0pprettVurderhenvendelseOppgave behov`() {
        testRapid.sendTestMessage(opprettVurderhenvendelseOppgaveBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertDoesNotThrow { field(0, "@løsning") }
            assertEquals(null, field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["fagsakId"].textValue())
            assertEquals("123", field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["oppgaveId"].asText())
            assertEquals(JOURNALPOST_ID, field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["journalpostId"].asText())
        }
    }

    //language=JSON
    private fun eksisterendeSakerBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "EksisterendeSaker"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "fnr": "12345678910"
        }
        """.trimIndent()

    //language=JSON
    private fun opprettStartVedtakBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettStartVedtakOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "fødselsnummer": "12345678910",
          "behandlendeEnhetId": "1235",
          "oppgavebeskrivelse": "beskrivende beskrivelse",
          "registrertDato": "${LocalDateTime.now()}",
          "tilleggsinformasjon": "I tillegg til informasjonen kommer det noen ganger tileggsinformasjon"
        }
        """.trimIndent()

    //language=JSON
    private fun opprettVurderhenvendelseOppgaveBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettVurderhenvendelseOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "fødselsnummer": "12345678910",
          "behandlendeEnhetId": "1235",
          "oppgavebeskrivelse": "beskrivende beskrivelse",
          "registrertDato": "${LocalDateTime.now()}",
          "tilleggsinformasjon": "I tillegg til informasjonen kommer det noen ganger tileggsinformasjon"
        }
        """.trimIndent()
}
