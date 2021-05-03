package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class PersonInformasjonTest {

    @ParameterizedTest
    @ValueSource(strings = ["STRENGT_FORTROLIG_UTLAND", "STRENGT_FORTROLIG"])
    fun `skal mappe diskresjonskoder`(kode: String) {
        val person = PersonInformasjon(
            Aktivitetslogg(),
            journalpostId = "12345",
            aktørId = "12345678",
            fødselsnummer = "12345678901",
            norskTilknytning = true,
            navn = "Test Testen",
            diskresjonskode = kode

        )
        assertTrue(person.person().diskresjonskode, "Kode $kode")
    }

    @Test
    fun `skal ikke ha diskresjonkode hvis ikke informasjon om det  `() {
        val person = PersonInformasjon(
            Aktivitetslogg(),
            journalpostId = "12345",
            aktørId = "12345678",
            fødselsnummer = "12345678901",
            norskTilknytning = true,
            navn = "Test Testen",
            diskresjonskode = null
        )
        assertFalse(person.person().diskresjonskode)
    }
}
