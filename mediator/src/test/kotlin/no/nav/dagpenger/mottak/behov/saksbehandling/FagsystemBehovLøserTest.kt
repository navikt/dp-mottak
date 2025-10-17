package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.assertions.throwables.shouldThrow
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
    private val søknadIdIkkeFerdigbehandlet = UUID.randomUUID()
    private val oppgaveRutingMock =
        mockk<OppgaveRuting>().also {
            coEvery { it.ruteOppgave(søknadsId = søknadIdIkkeFerdigbehandlet) } returns Fagsystem.Arena
            coEvery { it.ruteOppgave(søknadsId = søknadIdFerdigbehandlet) } returns Fagsystem.Dagpenger(sakId)
            coEvery { it.ruteOppgave(ident = testIdent) } returns Fagsystem.Dagpenger(sakId)
            coEvery { it.ruteOppgave(ident = testIdentUtenDagpengerSak) } returns Fagsystem.Arena
        }

    @BeforeEach
    fun `clear rapid`() {
        testRapid.reset()
    }

    @Test
    fun `Bestem fagsystem for ettersending til søknad som er ferdigbehandlet med vedtak i dp-sak`() {
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
    fun `Bestem fagsystem for ettersending til søknad som ikke er ferdigbehandlet med vedtak i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(bestemFagsystemBehovForEttersending(søknadIdIkkeFerdigbehandlet))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "ARENA"
        }
    }

    @Test
    fun `Bestem fagsystem for ettersending som mangler søknad-id`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(fagsystemBehovUtenSøknadId(testIdent, "ETTERSENDING"))
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

        testRapid.sendTestMessage(fagsystemBehovUtenSøknadId(testIdent, "KLAGE"))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "DAGPENGER"
        }
    }

    @Test
    fun `Skal kaste exception for ikke støttet kategori `() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        shouldThrow<RuntimeException> {
            testRapid.sendTestMessage(fagsystemBehovUtenSøknadId(testIdent, "GENERELL"))
        }
    }

    @Test
    fun `Bestem fagsystem for klage der person ikke har sak i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(fagsystemBehovUtenSøknadId(testIdentUtenDagpengerSak, "KLAGE"))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "ARENA"
        }
    }

    @Test
    fun `Bestem fagsystem for anke der person har sak i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(fagsystemBehovUtenSøknadId(testIdent, "ANKE"))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "DAGPENGER"
        }
    }

    @Test
    fun `Bestem fagsystem for anke der person ikke har sak i dp-sak`() {
        FagystemBehovLøser(
            oppgaveRuting = oppgaveRutingMock,
            rapidsConnection = testRapid,
        )

        testRapid.sendTestMessage(fagsystemBehovUtenSøknadId(testIdentUtenDagpengerSak, "ANKE"))
        with(testRapid.inspektør) {
            size shouldBe 1
            field(0, "@løsning")[FagystemBehovLøser.behovNavn]["fagsystem"].asText() shouldBe "ARENA"
        }
    }

    @Language("JSON")
    private fun fagsystemBehovUtenSøknadId(
        ident: String,
        kategori: String,
    ): String =
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
          "kategori": "$kategori"
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
