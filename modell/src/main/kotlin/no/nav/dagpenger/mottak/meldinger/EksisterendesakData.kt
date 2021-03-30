package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Hendelse

class EksisterendesakData(
    private val journalpostId: String,
    private val harEksisterendeSak: Boolean
) : Hendelse() {
    override fun journalpostId(): String = journalpostId
    fun harEksisterendeSaker(): Boolean = harEksisterendeSak
}
