package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Gjenopptak
import no.nav.dagpenger.mottak.NySøknad
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

internal class KategorisertJournalpostTest {
    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-01.03", "NAV 04-01.04"])
    fun `kategoriserer ny søknad`(brevkode: String) {
        assertTrue(lagjournalpostData(brevkode).kategorisertJournalpost() is NySøknad)
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-16.03", "NAV 04-16.04"])
    fun `kategoriserer gjennopptak`(brevkode: String) {
        assertTrue(lagjournalpostData(brevkode).kategorisertJournalpost() is Gjenopptak)
    }

    @Test
    fun `Tilleggsinformasjon generert`() {
        val dato = LocalDateTime.parse("2019-12-24T12:01:57")
        val informasjon = lagjournalpostData("NAV 04-16.03", dato).kategorisertJournalpost().oppgaveBenk(null).tilleggsinformasjon
        assertEquals(
            "Hoveddokument: Søknad om gjenopptak av dagpenger\n" +
                "- Vedlegg: Arbeidsavtale\n" +
                "Registrert dato: 24.12.2019\n" +
                "Dokumentet er skannet inn og journalført automatisk av digitale dagpenger. " +
                "Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\".",
            informasjon,
        )
    }

    @Test
    fun `Dropper tilleggsinformasjon over maks tegn satt`() {
        val dato = LocalDateTime.parse("2019-12-24T12:01:57")
        val informasjon =
            lagjournalpostData(
                "NAV 04-16.03",
                dato,
                getRandomString(3000),
            ).kategorisertJournalpost().oppgaveBenk(null).tilleggsinformasjon

        assertEquals(
            "Hoveddokument: Søknad om gjenopptak av dagpenger\n" +
                "Registrert dato: 24.12.2019\n" +
                "Dokumentet er skannet inn og journalført automatisk av digitale dagpenger. " +
                "Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\".",
            informasjon,
        )
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }
}
