package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingPeriode
import no.nav.dagpenger.mottak.api.Periode

class InMemoryInnsendingRepository : InnsendingRepository {
    private val innsendinger = mutableMapOf<String, Innsending>()

    override fun hent(journalpostId: String): Innsending? =
        innsendinger.getOrPut(journalpostId, { Innsending(journalpostId = journalpostId) })

    override fun lagre(innsending: Innsending): Int =
        with(innsendinger) {
            this[innsending.journalpostId()] = innsending
            this.size
        }

    override fun forPeriode(periode: Periode): List<InnsendingPeriode> {
        TODO("not implemented")
    }

    fun reset() = innsendinger.clear()
}
