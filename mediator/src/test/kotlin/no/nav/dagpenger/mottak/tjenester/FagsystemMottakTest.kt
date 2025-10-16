package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.Fagsystem.FagsystemType
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.FagsystemBesluttet
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class FagsystemMottakTest {
    private val testRapid = TestRapid()
    private val journalpostId = "12345"
    private val sakId = UUID.randomUUID()
    private val opprettetTidspunkt = LocalDateTime.now()

    @Test
    fun `Skal ta imot løsning på fagsystem`() {
        val slot = mutableListOf<FagsystemBesluttet>()
        val innsendingMediator =
            mockk<InnsendingMediator>().also {
                every { it.håndter(capture(slot)) } returns Unit
            }
        FagsystemMottak(
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
                    "BestemFagsystem"
                  ],
                  "journalpostId": "$journalpostId",
                  "@opprettet": "$opprettetTidspunkt",
                  "@løsning": {
                    "BestemFagsystem": {
                      "fagsakId": "$sakId",
                      "fagsystem": "DAGPENGER"
                    }
                  }
                }
                  
                """.trimIndent(),
        )

        slot.single().fagsystem.fagsystemType shouldBe FagsystemType.DAGPENGER
    }
}
