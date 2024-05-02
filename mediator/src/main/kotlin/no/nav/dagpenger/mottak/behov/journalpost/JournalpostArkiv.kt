package no.nav.dagpenger.mottak.behov.journalpost

internal interface JournalpostArkiv {
    suspend fun hentJournalpost(journalpostId: String): SafGraphQL.Journalpost
}
