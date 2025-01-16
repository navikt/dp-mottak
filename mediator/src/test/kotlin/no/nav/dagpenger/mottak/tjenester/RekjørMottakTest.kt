package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.RekjørHendelse
import kotlin.test.Test

internal class RekjørMottakTest {
    private val testRapid = TestRapid()
    private val innsendingMediator = mockk<InnsendingMediator>(relaxed = true)

    init {
        RekjørMottak(innsendingMediator, testRapid)
    }

    @Test
    fun `Skal håndtere rekjør hendelser`() {
        testRapid.sendTestMessage(rekjørHendelse())
        verify(exactly = 1) {
            innsendingMediator.håndter(any() as RekjørHendelse)
        }
    }

    private fun rekjørHendelse(): String {
        // language=JSON
        return """
            {
            "@event_name": "rekjør_innsending",
            "@id": "123e4567-e89b-12d3-a456-426614174000",
            "@opprettet": "2021-05-07T14:19:03.105592",
            "journalpostId": "506565476"
            }
            """.trimIndent()
    }
}
