package no.nav.dagpenger.mottak.tjenester

import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.db.InMemoryInnsendingRepository
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JournalføringMottakTest {

    private val testRapid = TestRapid()
    private val innsendingRepository = InMemoryInnsendingRepository()
    private val mediator = InnsendingMediator(
        innsendingRepository,
        testRapid
    )

    @Test
    fun `Skal motta joarkhendelse sende behov om Journalpost`() {
        val journalføringMottak = JournalføringMottak(
            mediator,
            testRapid
        )
        testRapid.sendTestMessage(joarkMelding())
        val behovMessage = testRapid.inspektør.message(0)
        assertEquals("Journalpost", behovMessage["@behov"].map { it.asText() }.first())
        assertEquals("124567", behovMessage["journalpostId"].asText())
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
