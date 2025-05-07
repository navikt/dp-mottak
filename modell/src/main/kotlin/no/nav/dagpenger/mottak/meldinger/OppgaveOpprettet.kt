package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import java.util.UUID

class OppgaveOpprettet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val oppgaveId: UUID,
    private val fagsakId: UUID,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun oppgaveSak(): OppgaveSak = OppgaveSak(oppgaveId, fagsakId)

    data class OppgaveSak(
        val oppgaveId: UUID,
        val sakId: UUID,
    )
}
