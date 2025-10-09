package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.DagpengerOppgaveOpprettet
import org.junit.jupiter.api.Test
import java.util.UUID

class DagpengerOppgaveOpprettetMottakTest {
    private val testRapid = TestRapid()
    private val journalpostId = "12345"
    private val sakId = UUID.randomUUID()
    private val oppgaveId = UUID.randomUUID()

    @Test
    fun `Skal ta i mot dagpenger oppgave opprettet løsning`() {
        val slot = mutableListOf<DagpengerOppgaveOpprettet>()
        val innsendingMediator =
            mockk<InnsendingMediator>().also {
                every { it.håndter(capture(slot)) } returns Unit
            }
        DagpengerOppgaveOpprettetMottak(
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
                    "OpprettDagpengerOppgave"
                  ],
                  "journalpostId": "$journalpostId",
                  "@løsning": {
                    "OpprettDagpengerOppgave": {
                      "fagsakId": "$sakId",
                      "oppgaveId": "$oppgaveId"
                    }
                  }
                }
                  
                """.trimIndent(),
        )

        slot.single().oppgaveSak().let {
            it.oppgaveId shouldBe oppgaveId
            it.fagsakId shouldBe sakId
        }
    }
}
