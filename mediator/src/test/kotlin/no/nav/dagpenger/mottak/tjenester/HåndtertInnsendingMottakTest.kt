package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.Fagsystem.FagsystemType
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.HåndtertInnsending
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class HåndtertInnsendingMottakTest {
    private val testRapid = TestRapid()
    private val journalpostId = "12345"
    private val sakId = UUID.randomUUID()
    private val opprettetTidspunkt = LocalDateTime.now()

    @Test
    fun `Skal ta imot løsning på håndtertInnsending`() {
        val slot = mutableListOf<HåndtertInnsending>()
        val innsendingMediator =
            mockk<InnsendingMediator>().also {
                every { it.håndter(capture(slot)) } returns Unit
            }
        HåndtertInnsendingMottak(
            rapidsConnection = testRapid,
            innsendingMediator = innsendingMediator,
        )

        testRapid.sendTestMessage(
            //language=json
            message =
                """
                {
                  "@event_name": "behov",
                  "@final": true,
                  "@id": "${UUID.randomUUID()}",
                  "@behov": [
                    "HåndterInnsending"
                  ],
                  "journalpostId": "$journalpostId",
                  "@opprettet": "$opprettetTidspunkt",
                  "@løsning": {
                    "HåndterInnsending": {
                      "sakId": "$sakId",
                      "håndtert": true
                    }
                  }
                }
                  
                """.trimIndent(),
        )

        testRapid.sendTestMessage(
            //language=json
            message =
                """
                {
                  "@event_name": "behov",
                  "@final": true,
                  "@id": "${UUID.randomUUID()}",
                  "@behov": [
                    "HåndterInnsending"
                  ],
                  "journalpostId": "$journalpostId",
                  "@opprettet": "$opprettetTidspunkt",
                  "@løsning": {
                    "HåndterInnsending": {
                      "håndtert": false
                    }
                  }
                }
                  
                """.trimIndent(),
        )

        slot.size shouldBe 2
        slot.first().let { håndtertInnsending ->
            håndtertInnsending.fagsystem.fagsystemType shouldBe FagsystemType.DAGPENGER
            (håndtertInnsending.fagsystem as Fagsystem.Dagpenger).sakId shouldBe sakId
        }
        slot.last().fagsystem.fagsystemType shouldBe FagsystemType.ARENA
    }
}
