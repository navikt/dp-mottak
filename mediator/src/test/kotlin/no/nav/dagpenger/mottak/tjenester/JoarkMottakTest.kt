package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class JoarkMottakTest {
    private val testRapid = TestRapid()
    private val mediator = mockk<InnsendingMediator>(relaxed = true)

    init {
        JoarkMottak(mediator, testRapid)
    }

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
    fun `skal skippe meldinger fra joark med andre hendelsetyper enn 'MidlertidigJournalført' og 'JournalpostMottatt'`() {
        testRapid.sendTestMessage(joarkMelding(hendelseType = "ANNEN"))
        verify(exactly = 0) { mediator.håndter(any() as JoarkHendelse) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["MidlertidigJournalført", "JournalpostMottatt"])
    fun `skal lese meldinger fra joark hendelsetyper 'MidlertidigJournalført' og 'JournalpostMottatt'`(hendelseType: String) {
        testRapid.sendTestMessage(joarkMelding(hendelseType = hendelseType))
        verify(exactly = 1) { mediator.håndter(any() as JoarkHendelse) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["EESSI", "NAV_NO_CHAT"])
    fun `skal skippe meldinger fra joark med mottakstype`(kanal: String) {
        testRapid.sendTestMessage(joarkMelding(mottaksKanal = kanal))
        verify(exactly = 0) { mediator.håndter(any() as JoarkHendelse) }
    }

    //language=JSON
    private fun joarkMelding(
        hendelseType: String = "MidlertidigJournalført",
        mottaksKanal: String = "NAV_NO",
        tema: String = "DAG",
    ): String =
        """
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
