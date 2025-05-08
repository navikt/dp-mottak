package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaOppslag
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.OpprettVedtakOppgaveResponse
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting.FagSystem.ARENA
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting.FagSystem.DAGPENGER
import no.nav.dagpenger.mottak.tjenester.asUUID
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class OppgaveBehovLøserTest {
    private val testRapid = TestRapid()
    private val journalpostId = "2345678"
    private val oppgaveId = UUID.randomUUID()
    private val ident = "12345678910"
    private val oppgaveKlientMock =
        mockk<OppgaveKlient>().also {
            coEvery {
                it.opprettOppgave(
                    fagsakId = any(),
                    journalpostId = journalpostId,
                    opprettetTidspunkt = any(),
                    ident = ident,
                    skjemaKategori = "Klage",
                )
            } returns
                oppgaveId
        }

    private val arenaOppslagMock =
        mockk<ArenaOppslag>().also {
            coEvery { it.opprettVurderHenvendelsOppgave(any(), any()) } returns
                OpprettVedtakOppgaveResponse(
                    fagsakId = null,
                    oppgaveId = "123",
                )
        }

    @BeforeEach
    fun `clear rapid`() {
        testRapid.reset()
    }

    @Test
    fun `Skal opprette oppgave i ARENA når oppgaverutingen svarer ARENA`() {
        OppgaveBehovLøser(
            arenaOppslag = arenaOppslagMock,
            oppgaveKlient = mockk(),
            oppgaveRuting =
                mockk<OppgaveRuting>().also {
                    every { it.ruteOppgave() } returns ARENA
                },
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(opprettOppgaveBehov())
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["fagsakId"].isNull shouldBe true
            field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["fagsystem"].asText() shouldBe ARENA.name
            field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["oppgaveId"].asText() shouldBe "123"
            field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["journalpostId"].asText() shouldBe journalpostId
        }
    }

    @Test
    fun `Skal opprette oppgave i DAGPENGER når oppgaverutingen svarer DAGPENGER`() {
        OppgaveBehovLøser(
            arenaOppslag = mockk(),
            oppgaveKlient = oppgaveKlientMock,
            oppgaveRuting =
                mockk<OppgaveRuting>().also {
                    every { it.ruteOppgave() } returns DAGPENGER
                },
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(opprettOppgaveBehov())
        with(testRapid.inspektør) {
            size shouldBe 1
            shouldNotThrowAny {
                field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["fagsakId"].asUUID()
            }
            field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["fagsystem"].asText() shouldBe DAGPENGER.name
            field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["oppgaveId"].asUUID() shouldBe oppgaveId
        }
    }

    @Test
    fun `Skal håndtere feil ved oppretting av oppgave i ARENA`() {
        OppgaveBehovLøser(
            arenaOppslag =
                mockk<ArenaOppslag>().also {
                    coEvery { it.opprettVurderHenvendelsOppgave(any(), any()) } returns null
                },
            oppgaveKlient = mockk(),
            oppgaveRuting =
                mockk<OppgaveRuting>().also {
                    every { it.ruteOppgave() } returns ARENA
                },
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(opprettOppgaveBehov())
        with(testRapid.inspektør) {
            size shouldBe 1
            shouldNotThrowAny { field(0, "@løsning") }
            field(0, "@løsning")[OppgaveBehovLøser.behovNavn]["@feil"].asText() shouldBe "Kunne ikke opprette Arena oppgave"
        }
    }

    @Test
    fun `Kaster exception når feil oppstår ved oppretting av oppgave i DAGPENGER`() {
        OppgaveBehovLøser(
            arenaOppslag = mockk(),
            oppgaveKlient =
                mockk<OppgaveKlient>().also {
                    coEvery {
                        it.opprettOppgave(
                            fagsakId = any(),
                            journalpostId = journalpostId,
                            opprettetTidspunkt = any(),
                            ident = ident,
                            skjemaKategori = "Klage",
                        )
                    } throws RuntimeException("Feil ved oppretting av oppgave")
                },
            oppgaveRuting =
                mockk<OppgaveRuting>().also {
                    every { it.ruteOppgave() } returns DAGPENGER
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
            "OpprettOppgave"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId",
          "fødselsnummer": "$ident",
          "behandlendeEnhetId": "1235",
          "oppgavebeskrivelse": "beskrivende beskrivelse",
          "registrertDato": "${LocalDateTime.now()}",
          "tilleggsinformasjon": "I tillegg til informasjonen kommer det noen ganger tileggsinformasjon",
          "skjemaKategori": "Klage"
        }
        """.trimIndent()
}
