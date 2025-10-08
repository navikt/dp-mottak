package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.System

class FagsystemBesluttet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    val system: System,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
