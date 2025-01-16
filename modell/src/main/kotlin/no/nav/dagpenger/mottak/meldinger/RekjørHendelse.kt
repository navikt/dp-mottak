package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class RekjørHendelse(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
