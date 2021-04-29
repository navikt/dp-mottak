package no.nav.dagpenger.mottak.behov.journalpost

import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class FerdigstillJournalpostBehovLøserTest {
    private val testRapid = TestRapid()
    private val journalpostDokarkiv: JournalpostDokarkiv = mockk()
    private val journalpostId = "23456789"
    init {
        FerdigstillJournalpostBehovLøser(
            journalpostDokarkiv,
            rapidsConnection = testRapid
        )
    }

    @Test
    fun `Løser behov FerdigstillJournalpost `() {
        coEvery {
            journalpostDokarkiv.ferdigstill(journalpostId)
        } just Runs

        testRapid.sendTestMessage(ferdigstillBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["FerdigstillJournalpost"])
            assertEquals(journalpostId, field(0, "@løsning")["FerdigstillJournalpost"]["journalpostId"].asText())
        }
    }

    //language=JSON
    private fun ferdigstillBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "FerdigstillJournalpost"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId"
        }
        """.trimIndent()
}
