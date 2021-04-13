package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.db.InMemoryInnsendingRepository
import no.nav.dagpenger.mottak.tjenester.EksisterendeSakerMottak
import no.nav.dagpenger.mottak.tjenester.JournalføringMottak
import no.nav.dagpenger.mottak.tjenester.JournalpostFerdigstiltMottak
import no.nav.dagpenger.mottak.tjenester.JournalpostMottak
import no.nav.dagpenger.mottak.tjenester.JournalpostOppdatertMottak
import no.nav.dagpenger.mottak.tjenester.MinsteinntektVurderingMottak
import no.nav.dagpenger.mottak.tjenester.OpprettStartVedtakOppgaveMottak
import no.nav.dagpenger.mottak.tjenester.PersondataMottak
import no.nav.dagpenger.mottak.tjenester.SøknadsdataMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime.now
import java.util.UUID

internal class MediatorE2ETest {

    private companion object {
        val JOURNALPOST_ID = 124567
    }

    private val testRapid = TestRapid()
    private val innsendingRepository = InMemoryInnsendingRepository()
    private val testObservatør = TestObservatør()
    private val mediator = InnsendingMediator(
        innsendingRepository = innsendingRepository,
        rapidsConnection = testRapid,
        observatører = listOf(testObservatør)
    )

    init {
        JournalføringMottak(mediator, testRapid)
        JournalpostMottak(mediator, testRapid)
        PersondataMottak(mediator, testRapid)
        SøknadsdataMottak(mediator, testRapid)
        MinsteinntektVurderingMottak(mediator, testRapid)
        EksisterendeSakerMottak(mediator, testRapid)
        OpprettStartVedtakOppgaveMottak(mediator, testRapid)
        JournalpostOppdatertMottak(mediator, testRapid)
        JournalpostFerdigstiltMottak(mediator, testRapid)
    }

    @Test
    fun `Skal motta hendelser og sende ut behov`() {
        håndterHendelse(joarkMelding())
        assertBehov("Journalpost", 0)
        håndterHendelse(journalpostMottattHendelse())
        assertBehov("Persondata", 1)
        håndterHendelse(persondataMottattHendelse())
        assertBehov("Søknadsdata", 2)
        håndterHendelse(søknadsdataMottakHendelse())
        assertBehov("MinsteinntektVurdering", 3)
        håndterHendelse(minsteinntektVurderingMotattHendelse())
        assertBehov("EksisterendeSaker", 4)
        håndterHendelse(eksisterendeSakerMotattHendelse())
        assertBehov("OpprettStartVedtakOppgave", 5)
        håndterHendelse(opprettStartVedtakMotattHendelse())
        assertBehov("OppdaterJournalpost", 6)
        håndterHendelse(oppdatertJournalpostMotattHendelse())
        assertBehov("FerdigstillJournalpost", 7)
        håndterHendelse(ferdigstiltJournalpostMotattHendelse())
        assertTrue(testRapid.inspektør.size == 8, "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}")
        assertEquals(testObservatør.tilstander.last(), InnsendingTilstandType.InnsendingFerdigstiltType)
    }

    private fun assertBehov(expectedBehov: String, indexPåMelding: Int) {
        assertTrue(testRapid.inspektør.size == indexPåMelding + 1, "Ingen melding på index $indexPåMelding")
        testRapid.inspektør.message(indexPåMelding).also { jsonNode ->
            assertEquals(expectedBehov, jsonNode["@behov"].map { it.asText() }.first())
            assertEquals(JOURNALPOST_ID.toString(), jsonNode["journalpostId"].asText())
        }
    }

    private fun håndterHendelse(melding: String) {
        testRapid.sendTestMessage(melding)
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
          "journalpostId": "$JOURNALPOST_ID",
          "@løsning": {
            "Journalpost": {
                "id" : "$JOURNALPOST_ID",
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
          "journalpostId": "$JOURNALPOST_ID",
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
          "journalpostId": "$JOURNALPOST_ID",
          "journalpostStatus": "Mottatt",
          "temaGammelt": "DAG",
          "temaNytt": "DAG",
          "mottaksKanal": "NAV_NO",
          "kanalReferanseId": "vetikke"
        }
    """.trimIndent()

    //language=JSON
    private fun søknadsdataMottakHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Søknadsdata"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "@løsning": {
            "Søknadsdata": {
              "søknadsId": "tadda",
              "data": {
                 "id": "blabla",
                 "masse": "masse data"
              }
            }
          }
        }
        """.trimIndent()

    // todo: Koronaregelverk?
    //language=JSON
    private fun minsteinntektVurderingMotattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "MinsteinntektVurdering"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "@løsning": {
            "MinsteinntektVurdering": {
              "oppfyllerMinsteArbeidsinntekt": null
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun eksisterendeSakerMotattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "MinsteinntektVurdering"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "@løsning": {
            "EksisterendeSaker": {
              "harEksisterendeSak": false
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun opprettStartVedtakMotattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettStartVedtakOppgave"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "@løsning": {
            "OpprettStartVedtakOppgave": {
              "fagsakId": "12345",
              "oppgaveId": "12345678"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun oppdatertJournalpostMotattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OppdaterJournalpost"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "@løsning": {
            "OppdaterJournalpost": {
              "journalpostId": "$JOURNALPOST_ID"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun ferdigstiltJournalpostMotattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "FerdigstillJournalpost"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "@løsning": {
            "FerdigstillJournalpost": {
              "journalpostId": "$JOURNALPOST_ID"
            }
          }
        }
        """.trimIndent()
}
