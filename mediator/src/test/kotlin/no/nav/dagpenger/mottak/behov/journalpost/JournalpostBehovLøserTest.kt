package no.nav.dagpenger.mottak.behov.journalpost

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class JournalpostBehovLøserTest {

    private companion object {
        val JOURNALPOST_ID = 124567
    }

    private val testRapid = TestRapid()

    private val journalpostBehovLøser = JournalpostBehovLøser(
        rapidsConnection = testRapid
    )

    @Test
    fun `test `() {
        testRapid.sendTestMessage(journalpostBehov())



    }

    //language=JSON
    private fun journalpostBehov() : String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Journalpost"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID"
        }
        """.trimIndent()

}