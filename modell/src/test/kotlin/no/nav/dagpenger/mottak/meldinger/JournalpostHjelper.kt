package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import java.time.LocalDateTime

internal fun lagjournalpostData(
    brevkode: String,
    dato: LocalDateTime = LocalDateTime.now(),
    vedlegg: String = "Vedlegg: Arbeidsavtale",
    behandlingstema: String? = null
): JournalpostData {
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
        ),
        behandlingstema = behandlingstema
    )
    return journalpostData
}
