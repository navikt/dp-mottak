package no.nav.dagpenger.mottak.meldinger

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime

internal class JournalpostDataTest {

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-01.03", "NAV 04-01.04"])
    fun `kategoriserer ny søknad`(brevkode: String) {
        assertTrue(lagjournalpostData(brevkode).journalpost() is JournalpostData.KategorisertJournalpost.NySøknad)
    }

    private fun lagjournalpostData(brevkode: String): JournalpostData {
        val journalpostData = JournalpostData(
            journalpostId = "1234",
            journalpostStatus = "MOTTATT",
            aktørId = "21",
            relevanteDatoer = listOf(
                JournalpostData.RelevantDato(LocalDateTime.now().toString(), JournalpostData.Datotype.DATO_REGISTRERT)
            ),
            dokumenter = listOf(
                JournalpostData.DokumentInfo(
                    kanskjetittel = null,
                    dokumentInfoId = "1223",
                    brevkode = brevkode
                ),
                JournalpostData.DokumentInfo(
                    kanskjetittel = "tittel",
                    dokumentInfoId = "12234",
                    brevkode = "N6"
                )
            )
        )
        return journalpostData
    }
}
