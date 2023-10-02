package no.nav.dagpenger.mottak.tjenester

import io.mockk.every
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class JournalpostMottakTest {
    private val testRapid = TestRapid()
    private val innsendingMediator = mockk<InnsendingMediator>(relaxed = true)

    init {
        JournalpostMottak(innsendingMediator, testRapid)
    }

    @Test
    fun `Skal håndtere at bruker er null`() {
        val slot = slot<Journalpost>()
        every { innsendingMediator.håndter(journalpost = capture(slot)) } returns Unit

        testRapid.sendTestMessage(journalpostUtenBruker())
        verify { innsendingMediator.håndter(any() as Journalpost) }

        assertTrue(slot.isCaptured)
        with(slot.captured) {
            assertNull(bruker())
            assertNotNull(hovedskjema())
        }
    }

    private fun journalpostUtenBruker(): String =
        //language=JSON
        """
        {
          "@event_name": "behov",
          "@opprettet": "2021-05-07T14:19:03.105592",
          "@id": "ac432da1-2914-43cc-a92f-791da0544c49",
          "@behov": [
            "Journalpost"
          ],
          "journalpostId": "506565476",
          "@løsning": {
            "Journalpost": {
              "journalstatus": "MOTTATT",
              "journalpostId": "506565476",
              "bruker": null,
              "tittel": null,
              "datoOpprettet": "2021-05-07T13:45:51",
              "journalfoerendeEnhet": "4416",
              "relevanteDatoer": [
                {
                  "dato": "2021-05-07T13:45:51",
                  "datotype": "DATO_DOKUMENT"
                },
                {
                  "dato": "2021-05-06T02:00",
                  "datotype": "DATO_REGISTRERT"
                }
              ],
              "dokumenter": [
                {
                  "tittel": "Bekreftelse på sluttårsak/nedsatt arbeidstid (ikke permittert)",
                  "dokumentInfoId": "529249966",
                  "brevkode": "NAV 04-08.03",
                  "hovedDokument": true
                }
              ],
              "behandlingstema": null
            }
          }
        }
        """.trimIndent()
}
