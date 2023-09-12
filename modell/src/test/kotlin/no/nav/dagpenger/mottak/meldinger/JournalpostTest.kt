package no.nav.dagpenger.mottak.meldinger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class JournalpostTest {
    @Test
    fun `skal bruke tittel hvis ikke null eller 'null'`() {
        assertEquals("Søknad om gjenopptak av dagpenger", dokumentInfo(null).tittel)
        assertEquals("Søknad om gjenopptak av dagpenger", dokumentInfo("null").tittel)
        assertEquals("Ukjent dokumenttittel", dokumentInfo(null, "ukjent-brevkode").tittel)
    }

    private fun dokumentInfo(tittel: String?, brevkode: String = "NAV 04-16.03") = Journalpost.DokumentInfo(
        tittel,
        "1",
        brevkode,
        true,
    )
}
