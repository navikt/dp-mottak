package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.OppgaveSakVisitor
import java.util.UUID

class OppgaveOpprettet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val oppgaveId: UUID?,
    private val fagsakId: UUID,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun oppgaveSak(): OppgaveSak = OppgaveSak(oppgaveId, fagsakId)

    data class OppgaveSak(
        val oppgaveId: UUID?,
        val fagsakId: UUID,
    ) {
        fun accept(visitor: OppgaveSakVisitor) {
            visitor.visitOppgaveSak(oppgaveId, fagsakId)
        }
    }
}
