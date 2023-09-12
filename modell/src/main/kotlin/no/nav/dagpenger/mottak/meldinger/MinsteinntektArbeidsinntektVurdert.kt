package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class MinsteinntektArbeidsinntektVurdert(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val oppfyllerMinsteArbeidsinntekt: Boolean?,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
    fun oppfyllerMinsteArbeidsinntekt(): Boolean? = oppfyllerMinsteArbeidsinntekt
}
