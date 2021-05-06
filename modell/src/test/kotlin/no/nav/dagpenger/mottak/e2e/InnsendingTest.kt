package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.EksisterendeSaker
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.FerdigstillJournalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.MinsteinntektVurdering
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OppdaterJournalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettGosysoppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettStartVedtakOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettVurderhenvendelseOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Søknadsdata
import no.nav.dagpenger.mottak.InnsendingTilstandType.AventerArenaOppgaveType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AventerArenaStartVedtakType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerFerdigstillJournalpostType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerGosysType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerJournalpostType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerMinsteinntektVurderingType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerPersondataType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerSvarOmEksisterendeSakerType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerSøknadsdataType
import no.nav.dagpenger.mottak.InnsendingTilstandType.InnsendingFerdigstiltType
import no.nav.dagpenger.mottak.InnsendingTilstandType.KategoriseringType
import no.nav.dagpenger.mottak.InnsendingTilstandType.MottattType
import no.nav.dagpenger.mottak.InnsendingTilstandType.UkjentBrukerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
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
        assertBehovDetaljer(Journalpost)

        håndterPersonInformasjon()
        assertBehovDetaljer(Persondata, setOf("brukerId"))

        håndterSøknadsdata()
        assertBehovDetaljer(Søknadsdata, setOf("dokumentInfoId"))

        håndterMinsteinntektVurderingData()
        assertBehovDetaljer(MinsteinntektVurdering, setOf("aktørId"))

        håndterEksisterendesakData()
        assertBehovDetaljer(EksisterendeSaker, setOf("fnr"))

        håndterArenaOppgaveOpprettet()
        assertBehovDetaljer(
            OpprettStartVedtakOppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon"
            )
        )

        håndterJournalpostOppdatert()
        assertBehovDetaljer(
            OppdaterJournalpost,
            setOf(
                "aktørId",
                "fødselsnummer",
                "navn",
                "fagsakId",
                "tittel",
                "dokumenter"
            )
        )

        håndterJournalpostFerdigstilt()
        assertBehovDetaljer(FerdigstillJournalpost)

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerSøknadsdataType,
            AvventerMinsteinntektVurderingType,
            AvventerSvarOmEksisterendeSakerType,
            AventerArenaStartVedtakType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
        assertFerdigstilt {
            assertEquals("NySøknad", it.type.name)
            assertNotNull(it.søknadsData)
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-16.03", "NAV 04-16.04"])
    fun `skal håndtere joark hendelse der journalpost er gjenopptak`(brevkode: String) {
        håndterJoarkHendelse()

        håndterJournalpostData(brevkode)
        assertBehovDetaljer(Journalpost)

        håndterPersonInformasjon()
        assertBehovDetaljer(Persondata, setOf("brukerId"))

        håndterSøknadsdata()
        assertBehovDetaljer(Søknadsdata, setOf("dokumentInfoId"))

        håndterArenaOppgaveOpprettet()
        assertBehovDetaljer(
            OpprettVurderhenvendelseOppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon"
            )
        )

        håndterJournalpostOppdatert()
        assertBehovDetaljer(
            OppdaterJournalpost,
            setOf(
                "aktørId",
                "fødselsnummer",
                "navn",
                "fagsakId",
                "tittel",
                "dokumenter"
            )
        )

        håndterJournalpostFerdigstilt()
        assertBehovDetaljer(FerdigstillJournalpost)

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerSøknadsdataType,
            AventerArenaOppgaveType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertEquals("Gjenopptak", it.type.name)
            assertNotNull(it.søknadsData)
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-06.08", "NAV 90-00.08", "NAV 04-06.05"])
    fun `skal håndtere joark hendelsene etablering, klage&anke og utdanning`(brevkode: String) {
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode)
        håndterPersonInformasjon()
        håndterArenaOppgaveOpprettet()
        assertBehovDetaljer(
            OpprettVurderhenvendelseOppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon"
            )
        )
        håndterJournalpostOppdatert()
        assertBehovDetaljer(
            OppdaterJournalpost,
            setOf(
                "aktørId",
                "fødselsnummer",
                "fagsakId",
                "navn",
                "tittel",
                "dokumenter"
            )
        )
        håndterJournalpostFerdigstilt()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AventerArenaOppgaveType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            val expected = setOf("Etablering", "KlageOgAnke", "Utdanning")
            assertTrue(it.type.name in expected, "Forventet at ${it.type.name} var en av $expected")
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAVe 04-16.04", "NAVe 04-16.03", "NAVe 04-01.03", "NAVe 04-01.04"])
    fun `skal håndtere joark hendelsene ettersending`(brevkode: String) {
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode)
        håndterPersonInformasjon()
        håndterSøknadsdata()
        håndterArenaOppgaveOpprettet()
        assertBehovDetaljer(
            OpprettVurderhenvendelseOppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon"
            )
        )
        håndterJournalpostOppdatert()
        assertBehovDetaljer(
            OppdaterJournalpost,
            setOf(
                "aktørId",
                "fødselsnummer",
                "fagsakId",
                "navn",
                "tittel",
                "dokumenter"
            )
        )
        håndterJournalpostFerdigstilt()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerSøknadsdataType,
            AventerArenaOppgaveType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertEquals("Ettersending", it.type.name)
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }
    }

    // "NAVe 04-16.04", "NAVe 04-16.03", "NAVe 04-01.03", "NAVe 04-01.04"

    @Test
    fun `skal håndtere ukjente brevkoder`() {
        håndterJoarkHendelse()
        håndterJournalpostData("Fritekstkode")
        håndterPersonInformasjon()
        håndterGosysOppgaveOpprettet()
        håndterJournalpostOppdatert()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerGosysType,
            InnsendingFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertEquals("UkjentSkjemaKode", it.type.name)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }
    }

    @Test
    fun `skal håndtere klage og anke for lønnskompensasjon`() {
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode = "NAV 90-00.08", behandlingstema = "ab0438")
        håndterPersonInformasjon()
        håndterGosysOppgaveOpprettet()
        håndterJournalpostOppdatert()
        assertBehovDetaljer(
            OpprettGosysoppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon"
            )
        )
        assertBehovDetaljer(
            OpprettGosysoppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon"
            )
        )
        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerGosysType,
            InnsendingFerdigstiltType
        )
        inspektør.also { it ->
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertEquals("KlageOgAnkeLønnskompensasjon", it.type.name)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
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
        assertBehovDetaljer(
            OpprettGosysoppgave,
            setOf(
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon"
            )
        )

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            KategoriseringType,
            UkjentBrukerType,
            InnsendingFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertNotNull(it.datoRegistrert)
        }
    }

    @Test
    fun `skal håndtere at informasjon om bruker ikke er funnet`() {

        håndterJoarkHendelse()
        håndterJournalpostData(brevkode = "NAVe 04-16.03")
        håndterPersonInformasjonIkkeFunnet()
        håndterGosysOppgaveOpprettet()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            UkjentBrukerType,
            InnsendingFerdigstiltType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertNotNull(it.datoRegistrert)
        }
    }

    @Test
    fun `skal forhindre at ferdigstilte joarkhendelser skal behandles på nytt`() {
        håndterJoarkHendelse()
        håndterJournalpostData("ukjent")
        håndterPersonInformasjon()
        håndterGosysOppgaveOpprettet()
        håndterJournalpostOppdatert()

        // joark hendelse med samme journalpostId kommer
        håndterJoarkHendelse()

        inspektør.also {
            assertTrue(it.innsendingLogg.hasErrors())
        }

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerGosysType,
            InnsendingFerdigstiltType
        )
    }
}
