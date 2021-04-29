package no.nav.dagpenger.mottak.behov.journalpost

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class OppdaterJournalpostBehovLøserTest {

    private val testRapid = TestRapid()
    private var mappedRequest: JournalpostApi.OppdaterJournalpostRequest? = null
    private val fagsakId = "23456"
    private val fødselsnummer = "12345678910"

    init {
        OppdaterJournalpostBehovLøser(
            journalpostOppdatering = object : JournalpostOppdatering {

                override suspend fun oppdaterJournalpost(
                    journalpostId: String,
                    journalpost: JournalpostApi.OppdaterJournalpostRequest
                ) {
                    mappedRequest = journalpost
                }
            },
            rapidsConnection = testRapid
        )
    }

    @Test
    fun `Løser oppdater journalpost behov`() {
        testRapid.sendTestMessage(journalpostBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["OppdaterJournalpost"])
            requireNotNull(mappedRequest).also {
                assertEquals(fagsakId, it.sak.fagsakId)
                assertEquals(JournalpostApi.SaksType.FAGSAK, it.sak.saksType)
                assertEquals(fødselsnummer, it.bruker.id)
                assertEquals(2, it.dokumenter.size)
            }
        }
    }

    @Test
    fun `Løser oppdater journalpost behov uten fagsakId`() {
        testRapid.sendTestMessage(journalpostBehovUtenFagsakId())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["OppdaterJournalpost"])
            requireNotNull(mappedRequest).also {
                assertEquals(JournalpostApi.SaksType.GENERELL_SAK, it.sak.saksType)
                assertEquals(fødselsnummer, it.bruker.id)
                assertEquals(2, it.dokumenter.size)
            }
        }
    }

    //language=JSON
    private fun journalpostBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OppdaterJournalpost"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "23456789",
          "fagsakId": "$fagsakId",
          "fødselsnummer": "$fødselsnummer",
          "aktørId": "123474",
          "tittel": "Søknad om permittering",
          "dokumenter": [{"dokumentInfoId": 1234, "tittel": "Her er en dokumentttittel"},{"dokumentInfoId": 1234, "tittel": "Her er en dokumentttittel"}]
        }
        """.trimIndent()

    //language=JSON
    private fun journalpostBehovUtenFagsakId(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OppdaterJournalpost"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "23456789",
          "fødselsnummer": "$fødselsnummer",
          "aktørId": "123474",
          "tittel": "Søknad om permittering",
          "dokumenter": [{"dokumentInfoId": 1234, "tittel": "Her er en dokumentttittel"},{"dokumentInfoId": 1234, "tittel": "Her er en dokumentttittel"}]
        }
        """.trimIndent()
}
