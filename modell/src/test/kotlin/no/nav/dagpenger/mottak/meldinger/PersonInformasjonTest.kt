package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class PersonInformasjonTest {
    @ParameterizedTest
    @ValueSource(strings = ["1234", "xyz"])
    fun `Skal ikke validere riktig hvis ikke det er gyldig fødselsnummer eller dnummer`(ident: String) {
        val personInformasjon =
            PersonInformasjon(
                Aktivitetslogg(),
                journalpostId = "12345",
                aktørId = "12345678",
                ident = ident,
                norskTilknytning = true,
                navn = "Test Testen",
                diskresjonskode = null,
            )
        assertFalse(personInformasjon.validate())
    }

    @ParameterizedTest
    @ValueSource(strings = ["STRENGT_FORTROLIG_UTLAND", "STRENGT_FORTROLIG"])
    fun `skal mappe diskresjonskoder`(kode: String) {
        val person =
            PersonInformasjon(
                Aktivitetslogg(),
                journalpostId = "12345",
                aktørId = "12345678",
                ident = GENERERT_FØDSELSNUMMER,
                norskTilknytning = true,
                navn = "Test Testen",
                diskresjonskode = kode,
            )
        assertTrue(person.person().diskresjonskode, "Kode $kode")
    }

    @Test
    fun `skal ikke ha diskresjonkode hvis ikke informasjon om det  `() {
        val person =
            PersonInformasjon(
                Aktivitetslogg(),
                journalpostId = "12345",
                aktørId = "12345678",
                ident = GENERERT_FØDSELSNUMMER,
                norskTilknytning = true,
                navn = "Test Testen",
                diskresjonskode = null,
            )
        assertFalse(person.person().diskresjonskode)
    }

    @ParameterizedTest
    @ValueSource(strings = ["19508418372", "01234567890"])
    fun `Validering av identer`(ident: String) {
        assertDoesNotThrow {
            PersonInformasjon.Person(
                aktørId = "12345678",
                ident = ident,
                norskTilknytning = true,
                navn = "Test Testen",
                diskresjonskode = false,
                egenAnsatt = false,
            )
        }
    }
}
