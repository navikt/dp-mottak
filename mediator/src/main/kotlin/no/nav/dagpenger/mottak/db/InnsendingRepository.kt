package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending

interface InnsendingRepository {
    fun person(journalpostId: String): Innsending
    fun lagre(innsending: Innsending): Boolean
}
