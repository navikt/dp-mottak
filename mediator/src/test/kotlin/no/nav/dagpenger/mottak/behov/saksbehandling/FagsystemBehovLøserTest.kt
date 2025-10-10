package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.coEvery
import io.mockk.mockk
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting
import no.nav.dagpenger.mottak.serder.asUUID
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class FagsystemBehovLøserTest {
    private val testRapid = TestRapid()
    private val journalpostId = "2345678"
    private val testIdent = "12345678910"
    private val testIdentUtenDagpengerSak = "12121244444"
    private val sakId = UUID.randomUUID()
    private val søknadIdFerdigbehandlet = UUID.randomUUID()
    private val søknadIdUnderBehandling = UUID.randomUUID()
    private val søknadIdIkkePåstartet = UUID.randomUUID()
    private val oppgaveRutingMock =
        mockk<OppgaveRuting>().also {
            coEvery { it.ruteOppgave(testIdent, søknadIdUnderBehandling) } returns Fagsystem.Arena
            coEvery { it.ruteOppgave(testIdent, søknadIdIkkePåstartet) } returns Fagsystem.Arena
            coEvery { it.ruteOppgave(testIdent, søknadIdFerdigbehandlet) } returns Fagsystem.Dagpenger(sakId)
            coEvery { it.ruteOppgave(testIdent) } returns Fagsystem.Dagpenger(sakId)
            coEvery { it.ruteOppgave(testIdentUtenDagpengerSak) } returns Fagsystem.Arena
        }

    @BeforeEach
    fun `clear rapid`() {
        testRapid.reset()
    }

    @Test
    fun `Bestem fagsystem for ettersending til søknad som er ferdigbehandlet i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(bestemFagsystemBehovForEttersending(søknadIdFerdigbehandlet))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsakId"].asUUID() shouldBe sakId
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "DAGPENGER"
        }
    }

    @Test
    fun `Bestem fagsystem for ettersending til søknad som er under behandling i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(bestemFagsystemBehovForEttersending(søknadIdUnderBehandling))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "ARENA"
        }
    }

    @Test
    fun `Bestem fagsystem for klage der person har sak i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(bestemFagsystemBehovForKlage(testIdent))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "DAGPENGER"
        }
    }

    @Test
    fun `Bestem fagsystem for klage der person ikke har sak i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(bestemFagsystemBehovForKlage(testIdentUtenDagpengerSak))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "ARENA"
        }
    }

    @Language("JSON")
    private fun bestemFagsystemBehovForKlage(ident: String): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "BestemFagsystem"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId",
          "fødselsnummer": "$ident",
          "kategori": "KLAGE"
        }
        """.trimIndent()

    @Language("JSON")
    private fun bestemFagsystemBehovForEttersending(søknadId: UUID): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "BestemFagsystem"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId",
          "fødselsnummer": "$testIdent",
          "søknadsId": "$søknadId",
          "kategori": "ETTERSENDING"
        }
        """.trimIndent()
}
