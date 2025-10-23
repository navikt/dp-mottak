package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.Hendelse

class HåndtertHenvendelse(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    val fagsystem: Fagsystem,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
