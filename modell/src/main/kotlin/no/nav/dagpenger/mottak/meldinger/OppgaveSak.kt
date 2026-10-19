package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.OppgaveSakVisitor
import java.util.UUID

data class OppgaveSak(
    val oppgaveId: UUID?,
    val fagsakId: UUID,
) {
    fun accept(visitor: OppgaveSakVisitor) {
        visitor.visitOppgaveSak(oppgaveId, fagsakId)
    }
}
