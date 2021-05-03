package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

internal class OpprettGosysOppgaveLøserTest {
    val journalpostId = "23456789"
    val testRapid = TestRapid()

    init {
        OpprettGosysOppgaveLøser(
            gosysOppslag = object : GosysOppslag {
                override suspend fun opprettOppgave(oppgave: GosysOppgaveRequest): String = "dfghjkl"
            },
            rapidsConnection = testRapid
        )
    }

    @Test
    fun `Løser opprett gosys oppagve behov`() {
        testRapid.sendTestMessage(opprettGosysOppgaveBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertDoesNotThrow { field(0, "@løsning") }
            assertEquals("dfghjkl", field(0, "@løsning")["OpprettGosysoppgave"]["oppgaveId"].asText())
        }
    }

    @Test
    fun `Løser opprett gosys oppagve behov uten personinformasjon`() {
        testRapid.sendTestMessage(opprettGosysOppgaveBehovUtenPersondata())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertDoesNotThrow { field(0, "@løsning") }
            assertEquals("dfghjkl", field(0, "@løsning")["OpprettGosysoppgave"]["oppgaveId"].asText())
        }
    }

    //language=JSON
    private fun opprettGosysOppgaveBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettGosysoppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId" : "$journalpostId",
          "aktørId": "34567890",
          "registrertDato": "2021-05-03T14:29:00",
          "behandlendeEnhetId":"3458",
          "oppgavebeskrivelse":"ahfjkafhk"
        }
        """.trimIndent()

    //language=JSON
    private fun opprettGosysOppgaveBehovUtenPersondata(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettGosysoppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId" : "$journalpostId",
          "registrertDato": "2021-05-03T14:29:00",
          "behandlendeEnhetId":"4568"
        }
        """.trimIndent()
}
