package no.nav.dagpenger.mottak.meldinger

class KlageAnke(
    journalpostId: String,
    journalpostStatus: String,
    aktørId: String,
    dokumenter: List<DokumentInfo>
) :
    Journalpost(
        journalpostId,
        journalpostStatus,
        aktørId,
        dokumenter
    )
