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
        bruker = JournalpostData.Bruker(id = "1234", type = JournalpostData.BrukerType.AKTOERID),
        relevanteDatoer = listOf(
            JournalpostData.RelevantDato(dato.toString(), JournalpostData.Datotype.DATO_REGISTRERT)
        ),
        dokumenter = listOf(
            JournalpostData.DokumentInfo(
                tittelHvisTilgjengelig = null,
                dokumentInfoId = "1223",
                brevkode = brevkode
            ),
            JournalpostData.DokumentInfo(
                tittelHvisTilgjengelig = vedlegg,
                dokumentInfoId = "12234",
                brevkode = "N6"
            )
        ),
        behandlingstema = behandlingstema
    )
    return journalpostData
}
