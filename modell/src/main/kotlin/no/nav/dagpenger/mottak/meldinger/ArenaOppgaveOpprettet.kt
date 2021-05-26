package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.ArenaSakVisitor
import no.nav.dagpenger.mottak.Hendelse

class ArenaOppgaveOpprettet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val oppgaveId: String,
    private val fagsakId: String?
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
    fun arenaSak(): ArenaSak = ArenaSak(oppgaveId, fagsakId)

    class ArenaSak(
        val oppgaveId: String,
        val fagsakId: String?
    ) {
        fun accept(visitor: ArenaSakVisitor) {
            visitor.visitArenaSak(oppgaveId, fagsakId)
        }
    }
}
