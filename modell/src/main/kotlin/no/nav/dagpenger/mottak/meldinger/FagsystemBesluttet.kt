package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import java.util.UUID

class FagsystemBesluttet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val fagsystem: String,
    private val fagsakId: UUID? = null,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
