package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class EksisterendesakData(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val harEksisterendeSak: Boolean
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
    fun harEksisterendeSaker(): Boolean = harEksisterendeSak
}
