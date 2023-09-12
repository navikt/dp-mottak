package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class JournalpostOppdatert(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
