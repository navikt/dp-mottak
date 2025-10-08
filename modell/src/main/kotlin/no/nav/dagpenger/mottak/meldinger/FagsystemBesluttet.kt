package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.OppgaveSakVisitor
import java.util.UUID

class FagsystemBesluttet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
