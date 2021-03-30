package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Hendelse

class ArenaOppgaveOpprettet(
    private val journalpostId: String,
    private val oppgaveId: String,
    private val fagsakId: String
) : Hendelse() {
    override fun journalpostId(): String = journalpostId
    fun arenaSak(): ArenaSak = ArenaSak(oppgaveId, fagsakId)

    data class ArenaSak(
        val oppgaveId: String,
        val fagsakId: String
    )
}
