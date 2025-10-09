package no.nav.dagpenger.mottak.e2e

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.FerdigstillJournalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OppdaterJournalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettGosysoppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettStartVedtakOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettVurderhenvendelseOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Søknadsdata
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AlleredeBehandletType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AventerArenaOppgaveType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AventerArenaStartVedtakType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerFagsystem
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerFerdigstillJournalpostType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerGosysType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerJournalpostType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerOppgaveType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerPersondataType
import no.nav.dagpenger.mottak.InnsendingTilstandType.AvventerSøknadsdataType
import no.nav.dagpenger.mottak.InnsendingTilstandType.InnsendingFerdigstiltType
import no.nav.dagpenger.mottak.InnsendingTilstandType.KategoriseringType
import no.nav.dagpenger.mottak.InnsendingTilstandType.MottattType
import no.nav.dagpenger.mottak.InnsendingTilstandType.UkjentBrukerType
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import org.junit.jupiter.params.provider.ValueSource
import java.lang.IllegalArgumentException

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

        håndterArenaOppgaveOpprettet()
        assertBehovDetaljer(
            OpprettStartVedtakOppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon",
            ),
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
                "mottakskanal",
                "dokumenter",
            ),
        )

        val behov =
            inspektør.innsendingLogg.behov().find { it.type == OppdaterJournalpost }
        behov!!.detaljer()["mottakskanal"] shouldBe "NAV_NO"

        håndterJournalpostFerdigstilt()
        assertBehovDetaljer(FerdigstillJournalpost)

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerSøknadsdataType,
            AventerArenaStartVedtakType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType,
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
        assertMottatt {
            assertEquals("NySøknad", it.type.name)
            assertNotNull(it.søknadsData)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }
        assertFerdigstilt {
            assertEquals("NySøknad", it.type.name)
            assertNotNull(it.søknadsData)
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }

        assertPuml(brevkode)
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
                "tilleggsinformasjon",
            ),
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
                "mottakskanal",
                "dokumenter",
            ),
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
            InnsendingFerdigstiltType,
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

        assertPuml(brevkode)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-06.08", "NAV 04-06.05"])
    fun `skal håndtere joark hendelsene etablering og utdanning`(brevkode: String) {
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
                "tilleggsinformasjon",
            ),
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
                "mottakskanal",
                "dokumenter",
            ),
        )
        håndterJournalpostFerdigstilt()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AventerArenaOppgaveType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType,
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            val expected = setOf("Etablering", "Utdanning")
            assertTrue(it.type.name in expected, "Forventet at ${it.type.name} var en av $expected")
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }

        assertPuml(brevkode)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 90-00.08", "NAV 90-00.08 K", "NAVe 90-00.08 K"])
    fun `skal håndtere joark hendelsene for klage og anke, klage og ettersending`(brevkode: String) {
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode)
        håndterPersonInformasjon()
        håndterOppgaveOpprettet()
        assertBehovDetaljer(
            OpprettOppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon",
                "skjemaKategori",
            ),
        )
        håndterJournalpostOppdatert()
        assertBehovDetaljer(
            OppdaterJournalpost,
            setOf(
                "aktørId",
                "fødselsnummer",
                "fagsakId",
                "oppgaveId",
                "navn",
                "tittel",
                "mottakskanal",
                "dokumenter",
            ),
        )
        håndterJournalpostFerdigstilt()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerOppgaveType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType,
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            val expected = setOf("Klage")
            assertTrue(it.type.name in expected, "Forventet at ${it.type.name} var en av $expected")
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }

        if (brevkode == "NAV 90-00.08") {
            assertPuml(brevkode)
        }
    }

    @ParameterizedTest
    @CsvSource(
        "NAVe 04-16.04, ARENA",
        "NAVe 04-16.03, ARENA",
        "NAVe 04-01.03, ARENA",
        "NAVe 04-01.04, ARENA",
        "NAVe 04-16.04, DAGPENGER",
        "NAVe 04-16.03, DAGPENGER",
        "NAVe 04-01.03, DAGPENGER",
        "NAVe 04-01.04, DAGPENGER",
    )
    fun `skal håndtere joark hendelsene ettersending`(
        brevkode: String,
        fagsystemType: String,
    ) {
        val fagsystemType = Fagsystem.FagsystemType.valueOf(fagsystemType)
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode)
        håndterPersonInformasjon()
        håndterSøknadsdata()
        assertBehovDetaljer(
            type = Behovtype.Fagsystem,
            detaljer =
                setOf(
                    "kategori",
                    "fødselsnummer",
                    "journalpostId",
                    "søknadsId",
                ),
        )
        håndterFagsystemLøst(fagsystemType)
        if (fagsystemType == Fagsystem.FagsystemType.ARENA) {
            håndterArenaOppgaveOpprettet()
            assertBehovDetaljer(
                OpprettVurderhenvendelseOppgave,
                setOf(
                    "aktørId",
                    "fødselsnummer",
                    "behandlendeEnhetId",
                    "oppgavebeskrivelse",
                    "registrertDato",
                    "tilleggsinformasjon",
                ),
            )
        }
        håndterJournalpostOppdatert()
        assertBehovDetaljer(
            OppdaterJournalpost,
            setOf(
                "aktørId",
                "fødselsnummer",
                "fagsakId",
                "navn",
                "mottakskanal",
                "tittel",
                "dokumenter",
            ),
        )
        håndterJournalpostFerdigstilt()

        val forventedeTilstander: List<InnsendingTilstandType> =
            buildList {
                add(MottattType)
                add(AvventerJournalpostType)
                add(AvventerPersondataType)
                add(KategoriseringType)
                add(AvventerSøknadsdataType)
                add(AvventerFagsystem)
                if (fagsystemType == Fagsystem.FagsystemType.ARENA) {
                    add(AventerArenaOppgaveType)
                }
                add(AvventerFerdigstillJournalpostType)
                add(InnsendingFerdigstiltType)
            }

        assertTilstander(
            *forventedeTilstander.toTypedArray(),
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
            assertNotNull(it.søknadsData)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["GENERELL_INNSENDING"])
    fun `skal håndtere joark hendelsene generell innsending`(brevkode: String) {
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
                "tilleggsinformasjon",
            ),
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
                "mottakskanal",
                "dokumenter",
            ),
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
            InnsendingFerdigstiltType,
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertEquals("Generell", it.type.name)
            assertNotNull(it.fagsakId)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }

        assertPuml(brevkode)
    }

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
            InnsendingFerdigstiltType,
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

        assertPuml("Ukjente brevkoder")
    }

    @Test
    fun `skal bare behandle journalposter med status mottatt`() {
        håndterJoarkHendelse()
        håndterJournalpostData(journalpostStatus = "JOURNALFOERT")
        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AlleredeBehandletType,
        )

        assertPuml("JournalpostStatus annen enn MOTTATT")
    }

    @Test
    fun `skal håndtere klage og anke for forskudd`() {
        val brevkode = "NAV 90-00.08"
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode = "NAV 90-00.08", behandlingstema = "ab0451")
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
                "tilleggsinformasjon",
            ),
        )
        assertBehovDetaljer(
            OpprettGosysoppgave,
            setOf(
                "aktørId",
                "fødselsnummer",
                "behandlendeEnhetId",
                "oppgavebeskrivelse",
                "registrertDato",
                "tilleggsinformasjon",
            ),
        )
        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerGosysType,
            InnsendingFerdigstiltType,
        )
        inspektør.also { it ->
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertEquals("KlageOgAnkeForskudd", it.type.name)
            assertNotNull(it.aktørId)
            assertNotNull(it.fødselsnummer)
            assertNotNull(it.datoRegistrert)
        }

        assertPuml("$brevkode-forskudd")
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
            "GENERELL_INNSENDING",
            "ukjent",
        ],
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
                "tilleggsinformasjon",
            ),
        )

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            KategoriseringType,
            UkjentBrukerType,
            InnsendingFerdigstiltType,
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
            InnsendingFerdigstiltType,
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertNotNull(it.datoRegistrert)
        }

        assertPuml("Ukjent bruker")
    }

    @ValueSource(
        strings = [
            "ab0438|4450",
            "ab0451|4153",
            "ab0452|4450",
        ],
    )
    @ParameterizedTest
    fun `skal håndtere at informasjon om bruker ikke er funnet og skjema er klage`(behandlingstemaOgEnhet: String) {
        val (behandlingstema, forventetEnhet) = behandlingstemaOgEnhet.split("|")
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode = "NAV 90-00.08", behandlingstema = behandlingstema)
        håndterPersonInformasjonIkkeFunnet()
        håndterGosysOppgaveOpprettet()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            UkjentBrukerType,
            InnsendingFerdigstiltType,
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertNotNull(it.datoRegistrert)
            assertEquals(forventetEnhet, it.behandlendeEnhet)
        }
    }

    @ValueSource(
        strings = [
            "ab0438",
            "ab0451",
            "ab0452",
        ],
    )
    @ParameterizedTest
    fun `skal håndtere at informasjon om bruker ikke er funnet og skjema er anke`(behandlingstema: String) {
        håndterJoarkHendelse()
        håndterJournalpostData(brevkode = "NAV 90-00.08 A", behandlingstema = behandlingstema)
        håndterPersonInformasjonIkkeFunnet()
        håndterGosysOppgaveOpprettet()

        assertTilstander(
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            UkjentBrukerType,
            InnsendingFerdigstiltType,
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }

        assertFerdigstilt {
            assertNotNull(it.datoRegistrert)
            assertEquals("4270", it.behandlendeEnhet)
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
            InnsendingFerdigstiltType,
        )
        assertPuml("Ferdigstilte journalposter")
    }

    @Test
    fun `Skal ikke håndtere replay eventer for andre tilstander enn ferdigstilt`() {
        håndterJoarkHendelse()
        assertThrows<IllegalArgumentException> { hånderReplayFerdigstilt() }
    }
}
