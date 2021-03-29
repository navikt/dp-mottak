package no.nav.dagpenger.mottak.meldinger

class NySøknad(
    journalpostId: String,
    journalpostStatus: String,
    aktørId: String,
    dokumenter: List<DokumentInfo>
) : Journalpost(
    journalpostId,
    journalpostStatus,
    aktørId,
    dokumenter
)
