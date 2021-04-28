package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending
import kotlin.random.Random

class InMemoryInnsendingRepository : InnsendingRepository {

    private val innsendinger = mutableMapOf<String, Innsending>()
    override fun hent(journalpostId: String): Innsending =
        innsendinger.getOrPut(journalpostId, { Innsending(Random.nextLong(), journalpostId = journalpostId) })

    override fun lagre(innsending: Innsending): Int = with(innsendinger) {
        this[innsending.journalpostId()] = innsending
        this.size
    }
}
