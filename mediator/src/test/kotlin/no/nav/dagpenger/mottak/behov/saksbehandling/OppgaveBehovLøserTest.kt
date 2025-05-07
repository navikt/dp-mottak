package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.behov.saksbehandling.OppgaveRuting.FagSystem.ARENA
import no.nav.dagpenger.mottak.behov.saksbehandling.OppgaveRuting.FagSystem.DAGPENGER
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaOppslag
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.OpprettVedtakOppgaveResponse
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class OppgaveBehovLøserTest {
    private val testRapid = TestRapid()
    private val journalpostId = "2345678"

    private val arenaOppslagMock =
        mockk<ArenaOppslag>().also {
            coEvery { it.opprettStartVedtakOppgave(any(), any()) } returns
                OpprettVedtakOppgaveResponse(
                    fagsakId = "123",
                    oppgaveId = "123",
                )
            coEvery { it.opprettVurderHenvendelsOppgave(any(), any()) } returns
                OpprettVedtakOppgaveResponse(
                    fagsakId = null,
                    oppgaveId = "123",
                )
        }

    @Test
    fun `Skal rute til ARENA dersom oppgaverutingen tilsier det`() {
        OppgaveBehovLøser(
            arenaOppslag = arenaOppslagMock,
            oppgaveKlient = mockk(),
            oppgaveRuting =
                mockk<OppgaveRuting>().also {
                    every { it.ruteOpgave() } returns ARENA
                },
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(opprettOppgaveBehov())
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["fagsakId"].isNull shouldBe true
            field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["oppgaveId"].asText() shouldBe "123"
            field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["journalpostId"].asText() shouldBe journalpostId
        }
    }

    @Test
    fun `Skal rute til DAGPENGER dersom oppgaverutingen tilsier det`() {
        OppgaveBehovLøser(
            arenaOppslag = arenaOppslagMock,
            oppgaveKlient = mockk(),
            oppgaveRuting =
                mockk<OppgaveRuting>().also {
                    every { it.ruteOpgave() } returns DAGPENGER
                },
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(opprettOppgaveBehov())
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["fagsakId"].isNull shouldBe true
            field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["oppgaveId"].asText() shouldBe "123"
            field(0, "@løsning")["OpprettVurderhenvendelseOppgave"]["journalpostId"].asText() shouldBe journalpostId
        }
    }

    @Language("JSON")
    private fun opprettOppgaveBehov(): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId",
          "fødselsnummer": "12345678910",
          "behandlendeEnhetId": "1235",
          "oppgavebeskrivelse": "beskrivende beskrivelse",
          "registrertDato": "${LocalDateTime.now()}",
          "tilleggsinformasjon": "I tillegg til informasjonen kommer det noen ganger tileggsinformasjon",
          "skjemaKategori": "Klage"
        }
        """.trimIndent()
}
