package no.nav.dagpenger.mottak.meldinger

class Etablering(
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
