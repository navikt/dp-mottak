package no.nav.dagpenger.mottak.behov.vilkårtester

internal interface RegelApiClient {
    fun startMinsteinntektVurdering(aktørId: String, journalpostId: String)
}
