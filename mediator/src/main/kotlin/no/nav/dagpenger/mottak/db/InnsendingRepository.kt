package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending

interface InnsendingRepository {
    fun hent(journalpostId: String): Innsending
    fun lagre(innsending: Innsending): Int
}
