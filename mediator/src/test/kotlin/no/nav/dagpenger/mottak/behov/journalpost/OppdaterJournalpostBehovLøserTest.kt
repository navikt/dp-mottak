package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.nulls.shouldNotBeNull
import io.mockk.coEvery
import io.mockk.mockk
import io.mockk.slot
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class OppdaterJournalpostBehovLøserTest {
    private val testRapid = TestRapid()
    private val fagsakId = "23456"
    private val fødselsnummer = "12345678910"

    private val slot = slot<JournalpostApi.OppdaterJournalpostRequest>()
    private val mockedJournalpostDokarkiv: JournalpostDokarkiv =
        mockk<JournalpostDokarkiv>().also {
            coEvery {
                it.oppdaterJournalpost(
                    journalpostId = any(),
                    journalpost = capture(slot),
                    eksternReferanseId = any(),
                )
            } returns Unit
        }

    init {
        OppdaterJournalpostBehovLøser(
            journalpostDokarkiv = mockedJournalpostDokarkiv,
            rapidsConnection = testRapid,
        )
    }

    @Test
    fun `Løser oppdater journalpost behov`() {
        testRapid.sendTestMessage(journalpostBehov("12345"))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["OppdaterJournalpost"])
            assertTrue(slot.isCaptured)
            requireNotNull(slot.captured).also {
                assertEquals(fagsakId, it.sak.fagsakId)
                assertEquals(JournalpostApi.SaksType.FAGSAK, it.sak.saksType)
                assertEquals(fødselsnummer, it.bruker.id)
            }
        }
    }

    @Test
    fun `Skal ikke oppdatere avsender hvis mottakskanal er NAV_NO`() {
        testRapid.sendTestMessage(journalpostBehov("12345", mottakskanal = "NAV_NO"))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["OppdaterJournalpost"])
            assertTrue(slot.isCaptured)
            requireNotNull(slot.captured).also {
                it.avsenderMottaker.shouldBeNull()
            }
        }
    }

    @Test
    fun `Skal oppdatere avsender hvis mottakskanal er annet enn NAV_NO`() {
        testRapid.sendTestMessage(journalpostBehov("12345", mottakskanal = "SKAN_IM"))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["OppdaterJournalpost"])
            assertTrue(slot.isCaptured)
            requireNotNull(slot.captured).also {
                it.avsenderMottaker.shouldNotBeNull()
                assertEquals(2, it.dokumenter.size)
                assertEquals(fødselsnummer, it.avsenderMottaker?.id)
                assertEquals("FNR", it.avsenderMottaker?.idType)
            }
        }
    }

    @Test
    fun `Løser oppdater journalpost behov uten fagsakId`() {
        testRapid.sendTestMessage(journalpostBehovUtenFagsakId())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["OppdaterJournalpost"])
            assertTrue(slot.isCaptured)
            requireNotNull(slot.captured).also {
                assertEquals(JournalpostApi.SaksType.GENERELL_SAK, it.sak.saksType)
                assertEquals(fødselsnummer, it.bruker.id)
                assertEquals(2, it.dokumenter.size)
            }
        }
    }

    @Test
    fun `test kjente feil tilstander`() {
        coEvery {
            mockedJournalpostDokarkiv.oppdaterJournalpost(
                journalpostId = "12345",
                journalpost = any(),
                eksternReferanseId = any(),
            )
        } throws
            JournalpostFeil.JournalpostException(
                statusCode = 400,
                //language=JSON
                content = """{
  "timestamp": "2021-04-30T07:54:09.362+00:00",
  "status": 400,
  "error": "Bad Request",
  "message": "Bruker kan ikke oppdateres for journalpost med journalpostStatus=J og journalpostType=I.",
  "path": "/rest/journalpostapi/v1/journalpost/493358469"
}""",
            )
        testRapid.sendTestMessage(journalpostBehov("12345"))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["OppdaterJournalpost"])
        }
    }

    //language=JSON
    private fun journalpostBehov(
        journalpostId: String = "23456789",
        mottakskanal: String = "NAV_NO",
    ): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "OppdaterJournalpost"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId",
          "fagsakId": "$fagsakId",
          "fødselsnummer": "$fødselsnummer",
          "mottakskanal" : "$mottakskanal",
          "aktørId": "123474",
          "tittel": "Søknad om permittering",
          "dokumenter": [{"dokumentInfoId": 1234, "tittel": "Her er en dokumentttittel"},{"dokumentInfoId": 1234, "tittel": "Her er en dokumentttittel"}]
        }
        """.trimIndent()

    //language=JSON
    private fun journalpostBehovUtenFagsakId(): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
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
