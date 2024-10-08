package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.just
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDateTime
import java.util.UUID

internal class FerdigstillJournalpostBehovLøserTest {
    private val testRapid = TestRapid()
    private val journalpostDokarkiv: JournalpostDokarkiv = mockk()
    private val journalpostId = "23456789"

    init {
        FerdigstillJournalpostBehovLøser(
            journalpostDokarkiv,
            rapidsConnection = testRapid,
        )
    }

    @Test
    fun `Løser behov FerdigstillJournalpost `() {
        val behovId = UUID.randomUUID()
        coEvery {
            journalpostDokarkiv.ferdigstill(journalpostId, behovId.toString())
        } just Runs

        testRapid.sendTestMessage(ferdigstillBehov(behovId = behovId))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["FerdigstillJournalpost"])
            assertEquals(journalpostId, field(0, "@løsning")["FerdigstillJournalpost"]["journalpostId"].asText())
        }
    }

    @Test
    fun `Skal ignorere kjente feil`() {
        val ferdigstiller =
            FerdigstillJournalpostBehovLøser(
                journalpostDokarkiv,
                rapidsConnection = testRapid,
            )
        val exception =
            JournalpostFeil.JournalpostException(
                400,
                """
                {"timestamp":"2021-06-08T21:10:42.062+00:00","status":400,"error":"Bad Request","message":"Journalpost med journalpostId=508859937 er ikke midlertidig journalført","path":"/rest/journalpostapi/v1/journalpost/508859937/ferdigstill"}
                """.trimIndent(),
            )
        assertDoesNotThrow { ferdigstiller.ignorerKjenteTilstander(exception) }
    }

    //language=JSON
    private fun ferdigstillBehov(behovId: UUID = UUID.randomUUID()): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "$behovId",
          "@behov": [
            "FerdigstillJournalpost"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId"
        }
        """.trimIndent()
}
