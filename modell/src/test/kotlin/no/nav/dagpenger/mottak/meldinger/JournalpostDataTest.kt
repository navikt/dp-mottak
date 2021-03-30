package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

internal class JournalpostDataTest {

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-01.03", "NAV 04-01.04"])
    fun `kategoriserer ny søknad`(brevkode: String) {
        assertTrue(lagjournalpostData(brevkode).journalpost() is JournalpostData.KategorisertJournalpost.NySøknad)
    }

    @Test
    fun `Tilleggsinformasjon generert`() {
        val dato = LocalDateTime.parse("2019-12-24T12:01:57")
        val informasjon = lagjournalpostData("NAV 04-01.03", dato).journalpost().tilleggsinformasjon()
        assertEquals(
            "Hoveddokument: Søknad om dagpenger (ikke permittert)\n" +
                "- Vedlegg: Arbeidsavtale\n" +
                "Registrert dato: 24.12.2019\n" +
                "Dokumentet er skannet inn og journalført automatisk av digitale dagpenger. " +
                "Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\".",
            informasjon
        )
    }

    @Test
    fun `Dropper tilleggsinformasjon over maks tegn satt`() {
        val dato = LocalDateTime.parse("2019-12-24T12:01:57")
        val informasjon = lagjournalpostData("NAV 04-01.03", dato, getRandomString(3000)).journalpost().tilleggsinformasjon()

        assertEquals(
            "Hoveddokument: Søknad om dagpenger (ikke permittert)\n" +
                "Registrert dato: 24.12.2019\n" +
                "Dokumentet er skannet inn og journalført automatisk av digitale dagpenger. " +
                "Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\".",
            informasjon
        )
    }

    private fun getRandomString(length: Int): String {
        val allowedChars = ('A'..'Z') + ('a'..'z') + ('0'..'9')
        return (1..length)
            .map { allowedChars.random() }
            .joinToString("")
    }

    private fun lagjournalpostData(brevkode: String, dato: LocalDateTime = LocalDateTime.now(), vedlegg: String = "Vedlegg: Arbeidsavtale"): JournalpostData {
        val journalpostData = JournalpostData(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = "1234",
            journalpostStatus = "MOTTATT",
            aktørId = "21",
            relevanteDatoer = listOf(
                JournalpostData.RelevantDato(dato.toString(), JournalpostData.Datotype.DATO_REGISTRERT)
            ),
            dokumenter = listOf(
                JournalpostData.DokumentInfo(
                    kanskjetittel = null,
                    dokumentInfoId = "1223",
                    brevkode = brevkode
                ),
                JournalpostData.DokumentInfo(
                    kanskjetittel = vedlegg,
                    dokumentInfoId = "12234",
                    brevkode = "N6"
                )
            )
        )
        return journalpostData
    }
}
