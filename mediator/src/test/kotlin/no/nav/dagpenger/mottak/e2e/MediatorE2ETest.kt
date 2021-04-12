package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.db.InMemoryInnsendingRepository
import no.nav.dagpenger.mottak.tjenester.JournalføringMottak
import no.nav.dagpenger.mottak.tjenester.JournalpostMottak
import no.nav.dagpenger.mottak.tjenester.PersondataMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now
import java.util.UUID

internal class MediatorE2ETest {

    private companion object {
        val journalpostId = 124567
    }

    private val testRapid = TestRapid()
    private val innsendingRepository = InMemoryInnsendingRepository()
    private val mediator = InnsendingMediator(
        innsendingRepository,
        testRapid
    )

    init {
        JournalføringMottak(
            mediator,
            testRapid
        )
        JournalpostMottak(
            mediator,
            testRapid
        )
        PersondataMottak(
            mediator,
            testRapid
        )
    }

    @Test
    fun `Skal motta joarkhendelse sende behov om Journalpost`() {
        håndterJoarkHendelse()
        assertBehov("Journalpost", 0)
        håndterJournalpostHendelse()
        assertBehov("Persondata", 1)
        håndterPersondataHendelse()
        assertBehov("Søknadsdata", 2)
    }

    private fun assertBehov(expectedBehov: String, indexPåMelding: Int) {
        testRapid.inspektør.message(indexPåMelding).also { jsonNode ->
            assertEquals(expectedBehov, jsonNode["@behov"].map { it.asText() }.first())
            assertEquals("124567", jsonNode["journalpostId"].asText())
        }
    }

    private fun håndterJoarkHendelse() {
        testRapid.sendTestMessage(joarkMelding())
    }

    private fun håndterJournalpostHendelse() {
        testRapid.sendTestMessage(journalpostMottattHendelse())
    }

    private fun håndterPersondataHendelse() {
        testRapid.sendTestMessage(persondataMottattHendelse())
    }

    //language=JSON
    private fun journalpostMottattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Journalpost"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "Journalpost": {
                "id" : "$journalpostId",
                "bruker" : {
                  "id": "12345678901",
                  "type": "FNR"
                },
                "relevanteDatoer" : [
                    {
                      "dato" : "${now()}",
                      "datotype": "DATO_REGISTRERT"
                    }
                ],
                "dokumenter" : [
                  {
                    "tittel" : null,
                    "dokumentInfoId" : 1234,
                    "brevkode" : "NAV 04-01.03"
                  },
                   {
                    "tittel" : null,
                    "dokumentInfoId" : 5678,
                    "brevkode" : "N6"
                  }
                ],
                "behandlingstema" : null
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun persondataMottattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Persondata"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "Persondata": {
              "aktoerId": "tadda",
              "naturligIdent": "12345678910",
              "norskTilknytning": true,
              "diskresjonskode": null
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun joarkMelding(): String = """
        {
          "hendelsesId": "",
          "versjon": "",
          "hendelsesType": "",
          "journalpostId": "$journalpostId",
          "journalpostStatus": "Mottatt",
          "temaGammelt": "DAG",
          "temaNytt": "DAG",
          "mottaksKanal": "NAV_NO",
          "kanalReferanseId": "vetikke"
        }
    """.trimIndent()
}
