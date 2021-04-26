package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import java.time.LocalDateTime
import java.time.ZoneId

internal fun lagjournalpostData(
    brevkode: String,
    dato: LocalDateTime = LocalDateTime.now(),
    vedlegg: String = "Vedlegg: Arbeidsavtale",
    behandlingstema: String? = null
): Journalpost {
    val journalpostData = Journalpost(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = "1234",
        journalpostStatus = "MOTTATT",
        bruker = Journalpost.Bruker(id = "1234", type = Journalpost.BrukerType.AKTOERID),
        registrertDato = dato.atZone(ZoneId.of("Europe/Oslo")),
        dokumenter = listOf(
            Journalpost.DokumentInfo(
                tittelHvisTilgjengelig = null,
                dokumentInfoId = "1223",
                brevkode = brevkode
            ),
            Journalpost.DokumentInfo(
                tittelHvisTilgjengelig = vedlegg,
                dokumentInfoId = "12234",
                brevkode = "N6"
            )
        ),
        behandlingstema = behandlingstema
    )
    return journalpostData
}
