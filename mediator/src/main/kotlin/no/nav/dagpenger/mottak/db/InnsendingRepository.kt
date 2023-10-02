package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingPeriode
import no.nav.dagpenger.mottak.api.Periode

interface InnsendingRepository {
    fun hent(journalpostId: String): Innsending?

    fun lagre(innsending: Innsending): Int

    fun forPeriode(periode: Periode): List<InnsendingPeriode>
}
