package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import no.nav.dagpenger.mottak.Søknad
import no.nav.dagpenger.mottak.erFornyetRettighet
import no.nav.dagpenger.mottak.erGrenseArbeider
import no.nav.dagpenger.mottak.erPermittertFraFiskeForedling
import no.nav.dagpenger.mottak.harAvsluttetArbeidsforholdFraKonkurs
import no.nav.dagpenger.mottak.harAvtjentVerneplikt
import no.nav.dagpenger.mottak.harEøsArbeidsforhold
import no.nav.dagpenger.mottak.harInntektFraFangstOgFiske
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

class OppgavebenkTest {
    private val jp = lagjournalpostData("NAV 04-01.03").kategorisertJournalpost()
    private val person = PersonInformasjon.Person("12344", "12345678901", norskTilknytning = true, diskresjonskode = false)

    @Test
    fun `Finn riktig oppgave beskrivelse og benk når søker har eøs arbeidsforhold de siste 3 årene `() {
        withSøknad(
            harEøsArbeidsforhold = true,
            harAvtjentVerneplikt = true,
            harInntektFraFangstOgFiske = true,
            erGrenseArbeider = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = null, søknad = it)
            assertEquals("4470", oppgaveBenk.id)
            assertEquals("MULIG SAMMENLEGGING - EØS\n", oppgaveBenk.beskrivelse)
        }
    }

    @Test
    fun `Bruk original benk når bruker har diskresjonskode`() {
        withSøknad(
            harAvtjentVerneplikt = true,
            harInntektFraFangstOgFiske = true,
            erGrenseArbeider = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person.copy(diskresjonskode = true), søknad = it)
            assertEquals("2103", oppgaveBenk.id)
            assertEquals("Start Vedtaksbehandling - automatisk journalført.\n", oppgaveBenk.beskrivelse)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk når søker har avtjent verneplikt `() {
        withSøknad(
            harAvtjentVerneplikt = true,
            harInntektFraFangstOgFiske = true,
            erGrenseArbeider = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, søknad = it)
            assertEquals("4450", oppgaveBenk.id)
            assertEquals("VERNEPLIKT\n", oppgaveBenk.beskrivelse)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk når søker har inntekt fra fangst og fisk ordinær`() {
        withSøknad(
            harInntektFraFangstOgFiske = true,
            erGrenseArbeider = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, søknad = it)
            assertEquals("FANGST OG FISKE\n", oppgaveBenk.beskrivelse)
            assertEquals("4450", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse når søker er grensearbeider `() {
        withSøknad(
            erGrenseArbeider = true,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, søknad = it)
            assertEquals("EØS\n", oppgaveBenk.beskrivelse)
            assertEquals("4465", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse ved Konkurs `() {
        withSøknad(
            harEøsArbeidsforhold = false,
            harInntektFraFangstOgFiske = false,
            erGrenseArbeider = false,
            harAvsluttetArbeidsforholdFraKonkurs = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, søknad = it)
            assertEquals("Konkurs\n", oppgaveBenk.beskrivelse)
            assertEquals("4401", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk når søker er permittert fra fiskeforedling`() {
        withSøknad(
            erPermittertFraFiskeforedling = true
        ) {
            val oppgaveBenk = jp.oppgaveBenk(person = person, søknad = it)
            assertEquals("FISK\n", oppgaveBenk.beskrivelse)
            assertEquals("4454", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk ved når en IKKE oppfyller minsteinntekt ved ordninær`() {
        withSøknad {
            val oppgaveBenk = jp.oppgaveBenk(person = person, søknad = it, oppfyllerMinsteArbeidsinntekt = false)
            assertEquals("Minsteinntekt - mulig avslag\n", oppgaveBenk.beskrivelse)
            assertEquals("4451", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og benk ved oppfyller minsteinntekt ved permittering   `() {
        withSøknad {
            val oppgaveBenk = lagjournalpostData("NAV 04-01.04").kategorisertJournalpost()
                .oppgaveBenk(person = person, søknad = it, oppfyllerMinsteArbeidsinntekt = false)
            assertEquals("Minsteinntekt - mulig avslag\n", oppgaveBenk.beskrivelse)
            assertEquals("4456", oppgaveBenk.id)
        }
    }

    @Test
    fun `Finn riktig oppgave beskrivelse og person ikke har norsk tilknytning ved permittering`() {
        withSøknad {
            val oppgaveBenk = lagjournalpostData("NAV 04-01.04").kategorisertJournalpost()
                .oppgaveBenk(person = person.copy(norskTilknytning = false), søknad = it)
            assertEquals("Start Vedtaksbehandling - automatisk journalført.\n", oppgaveBenk.beskrivelse)
            assertEquals("4465", oppgaveBenk.id)
        }
    }
    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-01.03", "NAV 04-01.04", "NAV 04-16.03", "NAV 04-16.04"])
    fun `finner riktig benk for  fornyet rettighet`(brevkode: String) {
        withSøknad(erFornyetRettighet = true) {
            val oppgavebenk = lagjournalpostData(brevkode).kategorisertJournalpost().oppgaveBenk(person = person, søknad = it)
            assertEquals("4451", oppgavebenk.id)
            assertEquals("Anmodningsvedtak 538", oppgavebenk.beskrivelse)
            assertDoesNotThrow { jacksonObjectMapper().readTree(oppgavebenk.tilleggsinformasjon) }
        }
    }

    @Test
    fun `Finner riktig benk for klage og anke når behandligstema er lønnskompensasjon`() {
        val jp = lagjournalpostData(brevkode = "NAV 90-00.08", behandlingstema = "ab0438").kategorisertJournalpost()
        jp.oppgaveBenk(person = person, søknad = null, oppfyllerMinsteArbeidsinntekt = null).also {
            assertEquals("Klage og anke — Lønnskompensasjon\n", it.beskrivelse)
            assertEquals("4486", it.id)
        }
    }

    @Test
    @Disabled("TODO: Få inne koronaregler")
    fun `Finn riktig oppgave beskrivelse og benk ved oppfyller minsteinntekt ved korona regler`() {
        withSøknad {
            val oppgaveBenk = jp.oppgaveBenk(person = person, søknad = it, oppfyllerMinsteArbeidsinntekt = false)
            assertEquals("Minsteinntekt - mulig avslag - korona\n", oppgaveBenk.beskrivelse)
            assertEquals("4451", oppgaveBenk.id)
        }
    }

    private fun withSøknad(
        harEøsArbeidsforhold: Boolean = false,
        harAvtjentVerneplikt: Boolean = false,
        harInntektFraFangstOgFiske: Boolean = false,
        erGrenseArbeider: Boolean = false,
        harAvsluttetArbeidsforholdFraKonkurs: Boolean = false,
        erPermittertFraFiskeforedling: Boolean = false,
        erFornyetRettighet: Boolean = false,
        test: (søknad: Søknad) -> Unit
    ) {

        mockkStatic(
            "no.nav.dagpenger.mottak.SøknadKt"
        ) {
            val søknad = mockk<Søknad>(relaxed = false).also {
                every { it.harEøsArbeidsforhold() } returns harEøsArbeidsforhold
                every { it.harAvtjentVerneplikt() } returns harAvtjentVerneplikt
                every { it.harInntektFraFangstOgFiske() } returns harInntektFraFangstOgFiske
                every { it.erGrenseArbeider() } returns erGrenseArbeider
                every { it.harAvsluttetArbeidsforholdFraKonkurs() } returns harAvsluttetArbeidsforholdFraKonkurs
                every { it.erPermittertFraFiskeForedling() } returns erPermittertFraFiskeforedling
                every { it.erFornyetRettighet() } returns erFornyetRettighet
            }
            test(søknad)
        }
    }
}
