package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Gosysoppgave
import no.nav.dagpenger.mottak.InnsendingTilstandType.AventerArenaOppgaveType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AventerArenaStartVedtakType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerGosysType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerJournalpostType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerMinsteinntektVurderingType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerPersondataType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerSvarOmEksisterendeSakerType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerSøknadsdataType
import no.nav.dagpenger.mottak.InnsendingTilstandType.FerdigstillJournalpostType
import no.nav.dagpenger.mottak.InnsendingTilstandType.JournalpostFerdigstiltType
import no.nav.dagpenger.mottak.InnsendingTilstandType.KategoriseringType
import no.nav.dagpenger.mottak.InnsendingTilstandType.MottattType
import no.nav.dagpenger.mottak.InnsendingTilstandType.OppdaterJournalpostType
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class InnsendingTest : AbstractEndeTilEndeTest() {

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-01.03", "NAV 04-01.04"])
    fun `skal håndtere joark hendelse der journalpost er ny søknad`(brevkode: String) {
        håndterJoarkHendelse()

        håndterJournalpostData(brevkode)

        håndterPersonInformasjon()

        håndterSøknadsdata()

        håndterMinsteinntektVurderingData()

        håndterEksisterendesakData()

        håndterArenaOppgaveOpprettet()

        håndterJournalpostOppdatert()

        håndterJournalpostFerdigstilt()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerSøknadsdataType,
            AvventerMinsteinntektVurderingType,
            AvventerSvarOmEksisterendeSakerType,
            AventerArenaStartVedtakType,
            OppdaterJournalpostType,
            FerdigstillJournalpostType,
            JournalpostFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-16.03", "NAV 04-16.04"])
    fun `skal håndtere joark hendelse der journalpost er gjenopptak`(brevkode: String) {
        håndterJoarkHendelse()

        håndterJournalpostData(brevkode)

        håndterPersonInformasjon()

        håndterSøknadsdata()

        håndterArenaOppgaveOpprettet()

        håndterJournalpostOppdatert()

        håndterJournalpostFerdigstilt()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerSøknadsdataType,
            AventerArenaOppgaveType,
            OppdaterJournalpostType,
            FerdigstillJournalpostType,
            JournalpostFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-06.08", "NAV 90-00.08", "NAVe 04-16.04", "NAVe 04-16.03", "NAVe 04-01.03", "NAVe 04-01.04", "NAV 04-06.05"])
    fun `skal håndtere joark hendelsene etablering, klage, ettersending og utdanning`(brevkode: String) {
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode)
        håndterPersonInformasjon()
        håndterArenaOppgaveOpprettet()
        håndterJournalpostOppdatert()
        håndterJournalpostFerdigstilt()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AventerArenaOppgaveType,
            OppdaterJournalpostType,
            FerdigstillJournalpostType,
            JournalpostFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
    }

    @Test
    fun `skal håndtere ukjente brevkoder`() {
        håndterJoarkHendelse()
        håndterJournalpostData("Fritekstkode")
        håndterPersonInformasjon()
        håndterGosysOppgaveOpprettet()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerGosysType,
            JournalpostFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
    }

    @Test
    fun `skal håndtere klage og anke for lønnskompensasjon`() {
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode = "NAV 90-00.08", behandlingstema = "ab0438")
        håndterPersonInformasjon()
        håndterGosysOppgaveOpprettet()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerGosysType,
            JournalpostFerdigstiltType
        )
        inspektør.also { it ->
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
            val gosysBehov = it.innsendingLogg.behov().find { behov ->
                behov.type == Gosysoppgave
            }
            assertContains(
                listOf(
                    "fødselsnummer",
                    "behandlendeEnhetId",
                    "oppgavebeskrivelse",
                    "registrertDato",
                    "tilleggsinformasjon"
                ),
                gosysBehov!!.detaljer()
            )
        }
    }

    @ParameterizedTest
    @ValueSource(
        strings = [
            "NAV 04-01.03",
            "NAV 04-01.04",
            "NAV 04-16.03",
            "NAV 04-16.04",
            "NAV 04-06.08",
            "NAV 90-00.08",
            "NAVe 04-16.04",
            "NAVe 04-16.03",
            "NAVe 04-01.03",
            "NAVe 04-01.04",
            "NAV 04-06.05",
            "ukjent"
        ]
    )
    fun `skal håndtere journalpost uten bruker`(brevkode: String) {

        håndterJoarkHendelse()
        håndterJournalpostData(brevkode = brevkode, bruker = null)
        håndterGosysOppgaveOpprettet()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            KategoriseringType,
            AvventerGosysType,
            JournalpostFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
    }

    private fun assertContains(keys: List<String>, map: Map<String, Any>) {
        keys.forEach {
            assertTrue(map.containsKey(it), "Fant ikke nøkkel $it i $map ")
        }
    }
}
