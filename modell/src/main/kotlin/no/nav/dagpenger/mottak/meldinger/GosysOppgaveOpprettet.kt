package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class GosysOppgaveOpprettet(val journalpostId: String, val oppgaveId: String, aktivitetslogg: Aktivitetslogg = Aktivitetslogg()) : Hendelse(
    aktivitetslogg,
) {
    override fun journalpostId(): String = journalpostId
}
