package no.nav.dagpenger.mottak.tjenester

import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class JoarkMottakTest {

    private val testRapid = TestRapid()
    private val mediator = mockk<InnsendingMediator>(relaxed = true)
    private val mottak = JoarkMottak(mediator, testRapid)

    @Test
    fun `skal lese meldinger fra joark med tema DAG`() {
        testRapid.sendTestMessage(joarkMelding())
        verify(exactly = 1) { mediator.håndter(any() as JoarkHendelse) }
    }

    @Test
    fun `skal skippe meldinger fra joark med andre temaer enn DAG`() {
        testRapid.sendTestMessage(joarkMelding(tema = "ANNET"))
        verify(exactly = 0) { mediator.håndter(any() as JoarkHendelse) }
    }

    @Test
    fun `skal skippe meldinger fra joark med andre hendelsetyper enn 'MidlertidigJournalført'`() {
        testRapid.sendTestMessage(joarkMelding(hendelseType = "ANNEN"))
        verify(exactly = 0) { mediator.håndter(any() as JoarkHendelse) }
    }

    @Test
    fun `skal skippe meldinger fra joark med mottakstyoe 'EESSI'`() {
        testRapid.sendTestMessage(joarkMelding(mottaksKanal = "EESSI"))
        verify(exactly = 0) { mediator.håndter(any() as JoarkHendelse) }
    }

    //language=JSON
    private fun joarkMelding(hendelseType: String = "MidlertidigJournalført", mottaksKanal: String = "NAV_NO", tema: String = "DAG"): String = """
        {
          "hendelsesId": "",
          "versjon": "",
          "hendelsesType": "$hendelseType",
          "journalpostId": "123456789",
          "journalpostStatus": "Mottatt",
          "temaGammelt": "$tema",
          "temaNytt": "$tema",
          "mottaksKanal": "$mottaksKanal",
          "kanalReferanseId": "vetikke"
        }
    """.trimIndent()
}
