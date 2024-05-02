package no.nav.dagpenger.mottak.behov.journalpost

internal interface SøknadsArkiv {
    suspend fun hentSøknadsData(
        journalpostId: String,
        dokumentInfoId: String,
    ): SafGraphQL.SøknadsData
}
