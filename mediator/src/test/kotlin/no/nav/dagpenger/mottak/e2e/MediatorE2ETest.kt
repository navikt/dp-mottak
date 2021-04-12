package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.db.InMemoryInnsendingRepository
import no.nav.dagpenger.mottak.tjenester.JournalføringMottak
import no.nav.dagpenger.mottak.tjenester.JournalpostMottak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.UUID

internal class MediatorE2ETest {

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
    }
    @Test
    fun `Skal motta joarkhendelse sende behov om Journalpost`() {
        håndterJoarkHendelse()
        val behovMessage = testRapid.inspektør.message(0)
        assertEquals("Journalpost", behovMessage["@behov"].map { it.asText() }.first())
        assertEquals("124567", behovMessage["journalpostId"].asText())
        håndterJournalpostHendelse()
    }

    private fun håndterJournalpostHendelse() {
        testRapid.sendTestMessage(journalpostHendelse())
    }

    private fun journalpostHendelse(): String =
        """
            {
                "@id": "${UUID.randomUUID()}",
                
            }
            
        """.trimIndent()

    private fun håndterJoarkHendelse() {
        testRapid.sendTestMessage(joarkMelding())
    }

    //language=JSON
    private fun joarkMelding(): String = """
        {
          "hendelsesId": "",
          "versjon": "",
          "hendelsesType": "",
          "journalpostId": 124567,
          "journalpostStatus": "Mottatt",
          "temaGammelt": "DAG",
          "temaNytt": "DAG",
          "mottaksKanal": "NAV_NO",
          "kanalReferanseId": "vetikke"
        }
    """.trimIndent()
}
