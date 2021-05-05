package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class ArenaOppgaveFeilet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String
) : Hendelse() {
    override fun journalpostId(): String = journalpostId
}
