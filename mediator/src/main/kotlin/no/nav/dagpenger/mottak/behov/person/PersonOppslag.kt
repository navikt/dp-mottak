package no.nav.dagpenger.mottak.behov.person

internal interface PersonOppslag {
    suspend fun hentPerson(id: String): Pdl.Person
}
