package no.nav.dagpenger.mottak.meldinger

class Utdanning(
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
