package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending

class InMemoryInnsendingRepository : InnsendingRepository {

    private val innsendinger = mutableMapOf<String, Innsending>()
    override fun hent(journalpostId: String): Innsending =
        innsendinger.getOrPut(journalpostId, { Innsending(journalpostId = journalpostId) })

    override fun lagre(innsending: Innsending): Int = with(innsendinger) {
        this[innsending.journalpostId()] = innsending
        this.size
    }
}
