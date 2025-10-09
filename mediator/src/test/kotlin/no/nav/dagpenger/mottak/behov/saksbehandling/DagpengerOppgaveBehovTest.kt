package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.mottak.serder.asUUID
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class DagpengerOppgaveBehovTest {
    private val testRapid = TestRapid()
    private val journalpostId = "2345678"
    private val oppgaveId = UUID.randomUUID()
    private val ident = "12345678910"
    private val fagsakId = UUID.randomUUID()
    private val oppgaveKlientMock =
        mockk<SaksbehandlingKlient>().also {
            coEvery {
                it.opprettOppgave(
                    fagsakId = any(),
                    journalpostId = journalpostId,
                    opprettetTidspunkt = any(),
                    ident = ident,
                )
            } returns
                oppgaveId
        }

    @BeforeEach
    fun `clear rapid`() {
        testRapid.reset()
    }

    @Test
    fun `Skal opprette oppgave `() {
        DagpengerOppgaveBehovLøser(
            saksbehandlingKlient = oppgaveKlientMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(opprettOppgaveBehov())
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[DagpengerOppgaveBehovLøser.behovNavn]["fagsakId"].asUUID() shouldBe fagsakId
            field(0, "@løsning")[DagpengerOppgaveBehovLøser.behovNavn]["oppgaveId"].asUUID() shouldBe oppgaveId
        }
    }

    @Test
    fun `Kaster exception når feil oppstår ved oppretting av oppgave`() {
        DagpengerOppgaveBehovLøser(
            saksbehandlingKlient =
                mockk<SaksbehandlingKlient>().also {
                    coEvery {
                        it.opprettOppgave(
                            fagsakId = any(),
                            journalpostId = journalpostId,
                            opprettetTidspunkt = any(),
                            ident = ident,
                        )
                    } throws RuntimeException("Feil ved oppretting av oppgave")
                },
            rapidsConnection = testRapid,
        )

        shouldThrow<RuntimeException> { testRapid.sendTestMessage(opprettOppgaveBehov()) }
    }

    @Language("JSON")
    private fun opprettOppgaveBehov(): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettDagpengerOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId",
          "fødselsnummer": "$ident",
          "registrertDato": "${LocalDateTime.now()}",
          "fagsakId": "$fagsakId"
        }
        """.trimIndent()
}
