package no.nav.dagpenger.mottak.behov.saksbehandling

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private const val JOURNALPOST_ID = "2345678"

internal class EksisterendeSakerLøserTest {

    val testRapid = TestRapid()

    init {
        EksisterendeSakerLøser(
            arenaOppslag = object : ArenaOppslag {
                override suspend fun harEksisterendeSaker(fnr: String, virkningstidspunkt: LocalDate): Boolean = true
            },
            rapidsConnection = testRapid
        )
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

    // TODO: legge til fnr i eksisterendeSaker melding
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
}
