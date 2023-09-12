package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import java.time.LocalDateTime

internal fun lagjournalpostData(
    brevkode: String,
    dato: LocalDateTime = LocalDateTime.now(),
    vedlegg: String = "Vedlegg: Arbeidsavtale",
    behandlingstema: String? = null,
): Journalpost {
    val journalpostData = Journalpost(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = "1234",
        journalpostStatus = "MOTTATT",
        bruker = Journalpost.Bruker(id = "1234", type = Journalpost.BrukerType.AKTOERID),
        registrertDato = dato,
        dokumenter = listOf(
            Journalpost.DokumentInfo(
                tittelHvisTilgjengelig = null,
                dokumentInfoId = "1223",
                brevkode = brevkode,
                hovedDokument = true,
            ),
            Journalpost.DokumentInfo(
                tittelHvisTilgjengelig = vedlegg,
                dokumentInfoId = "12234",
                brevkode = "N6",
                hovedDokument = false,
            ),
        ),
        behandlingstema = behandlingstema,
    )
    return journalpostData
}
