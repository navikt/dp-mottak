package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import java.util.UUID

class DagpengerOppgaveOpprettet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val oppgaveId: UUID?,
    private val fagsakId: UUID,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun oppgaveSak(): OppgaveSak = OppgaveSak(oppgaveId, fagsakId)
}
