package no.nav.dagpenger.mottak.meldinger

import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import no.nav.dagpenger.mottak.RutingOppslag
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OppgavebenkTest {
    private val jp = lagjournalpostData("NAV 04-01.03").kategorisertJournalpost()
    private val person = PersonInformasjon.Person(
        "Test Navn",
        "12344",
        GENERERT_FØDSELSNUMMER,
        norskTilknytning = true,
        diskresjonskode = false
    )

    @Test
    fun `Finn riktig oppgave beskrivelse og benk når søker har eøs arbeidsforhold de siste 3 årene `() {
        withSøknad(
            harEøsArbeidsforhold = true,
            harAvtjentVerneplikt = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = null, rutingOppslag = it)
            assertEquals("4470", oppgaveBenk.id)
            assertEquals("MULIG SAMMENLEGGING - EØS\n", oppgaveBenk.beskrivelse)
        }
    }

    @Test
    fun `Bruk original benk når bruker har diskresjonskode`() {
        withSøknad(
            harAvtjentVerneplikt = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person.copy(diskresjonskode = true), rutingOppslag = it)
            assertEquals("2103", oppgaveBenk.id)
            assertEquals("Start Vedtaksbehandling - automatisk journalført.\n", oppgaveBenk.beskrivelse)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk når søker har avtjent verneplikt `() {
        withSøknad(
            harAvtjentVerneplikt = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, rutingOppslag = it)
            assertEquals("4450", oppgaveBenk.id)
            assertEquals("VERNEPLIKT\n", oppgaveBenk.beskrivelse)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse ved Konkurs `() {
        withSøknad(
            harEøsArbeidsforhold = false,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, rutingOppslag = it)
            assertEquals("Konkurs\n", oppgaveBenk.beskrivelse)
            assertEquals("4401", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig benk og oppgavebeskrivelse ved eøs bostedsland og permittert`() {
        withSøknad(
            harEøsBostedsland = true,
            erPermittert = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, rutingOppslag = it)
            assertEquals("EØS\n", oppgaveBenk.beskrivelse)
            assertEquals("4465", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig benk og oppgavebeskrivelse ved eøs bostedsland og ikke permittert`() {
        withSøknad(
            harEøsBostedsland = true,
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, rutingOppslag = it)
            assertEquals("Start Vedtaksbehandling - automatisk journalført.\n", oppgaveBenk.beskrivelse)
            assertEquals("4450", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig benk og oppgavebeskrivelse når eøs arbeidsforhold overskriver eøs bostedsland`() {
        withSøknad(
            harEøsArbeidsforhold = true,
            harEøsBostedsland = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, rutingOppslag = it)
            assertEquals("MULIG SAMMENLEGGING - EØS\n", oppgaveBenk.beskrivelse)
            assertEquals("4470", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk når søker er permittert fra fiskeforedling`() {
        withSøknad(
            erPermittertFraFiskeforedling = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, rutingOppslag = it)
            assertEquals("FISK\n", oppgaveBenk.beskrivelse)
            assertEquals("4450", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk ved når en IKKE oppfyller minsteinntekt ved ordinær`() {
        withSøknad {
            val oppgaveBenk = jp.oppgaveBenk(person = person, rutingOppslag = it, oppfyllerMinsteArbeidsinntekt = false)
            assertEquals("Minsteinntekt - mulig avslag\n", oppgaveBenk.beskrivelse)
            assertEquals("4450", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk ved oppfyller minsteinntekt ved permittering   `() {
        withSøknad {
            val oppgaveBenk = lagjournalpostData("NAV 04-01.04").kategorisertJournalpost()
                .oppgaveBenk(person = person, rutingOppslag = it, oppfyllerMinsteArbeidsinntekt = false)
            assertEquals("Minsteinntekt - mulig avslag\n", oppgaveBenk.beskrivelse)
            assertEquals("4450", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og person ikke har norsk tilknytning ved permittering`() {
        withSøknad {
            val oppgaveBenk = lagjournalpostData("NAV 04-01.04").kategorisertJournalpost()
                .oppgaveBenk(person = person.copy(norskTilknytning = false), rutingOppslag = it)
            assertEquals("Start Vedtaksbehandling - automatisk journalført.\n", oppgaveBenk.beskrivelse)
            assertEquals("4465", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finner riktig benk for klage og anke når behandligstema er lønnskompensasjon`() {
        val jp = lagjournalpostData(brevkode = "NAV 90-00.08", behandlingstema = "ab0438").kategorisertJournalpost()
        jp.oppgaveBenk(person = person, rutingOppslag = null, oppfyllerMinsteArbeidsinntekt = null).also {
            assertEquals("Klage og anke - Lønnskompensasjon\n", it.beskrivelse)
            assertEquals("4486", it.id)
        }
    }

    @Test
    fun `Finner riktig benk for klage og anke når behandligstema er forskudd`() {
        val jp = lagjournalpostData(brevkode = "NAV 90-00.08", behandlingstema = "ab0451").kategorisertJournalpost()
        jp.oppgaveBenk(person = person, rutingOppslag = null, oppfyllerMinsteArbeidsinntekt = null).also {
            assertEquals("Klage og anke - Forskudd\n", it.beskrivelse)
            assertEquals("4153", it.id)
        }
    }

    @Test
    fun `Finner riktig benk for klage og anke når behandligstema er feripenger dangper`() {
        val jp = lagjournalpostData(brevkode = "NAV 90-00.08", behandlingstema = "ab0452").kategorisertJournalpost()
        jp.oppgaveBenk(person = person, rutingOppslag = null, oppfyllerMinsteArbeidsinntekt = null).also {
            assertEquals("Klage og anke - Feriepenger\n", it.beskrivelse)
            assertEquals("4456", it.id)
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-02.01", "NAVe 04-02.01", "NAV 04-02.03", "NAV 04-02.05", "NAVe 04-02.05"])
    fun `finner riktig benk for brevkoder som skal til utlandet`(brevkode: String) {
        val oppgavebenk = lagjournalpostData(brevkode).kategorisertJournalpost().oppgaveBenk(person = person)
        assertEquals("4470", oppgavebenk.id)
    }

    private fun withSøknad(
        harEøsArbeidsforhold: Boolean = false,
        harAvtjentVerneplikt: Boolean = false,
        harEøsBostedsland: Boolean = false,
        harAvsluttetArbeidsforholdFraKonkurs: Boolean = false,
        erPermittertFraFiskeforedling: Boolean = false,
        erPermittert: Boolean = false,
        test: (søknadFakta: RutingOppslag) -> Unit
    ) {

        mockkStatic(
            "no.nav.dagpenger.mottak.SøknadFaktaKt"
        ) {
            val søknad = mockk<RutingOppslag>(relaxed = false).also {
                every { it.eøsArbeidsforhold() } returns harEøsArbeidsforhold
                every { it.avtjentVerneplikt() } returns harAvtjentVerneplikt
                every { it.eøsBostedsland() } returns harEøsBostedsland
                every { it.avsluttetArbeidsforholdFraKonkurs() } returns harAvsluttetArbeidsforholdFraKonkurs
                every { it.permittertFraFiskeForedling() } returns erPermittertFraFiskeforedling
                every { it.permittert() } returns erPermittert
            }
            test(søknad)
        }
    }
}
