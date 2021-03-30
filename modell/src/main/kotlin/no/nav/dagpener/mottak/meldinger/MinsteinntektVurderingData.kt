package no.nav.dagpener.mottak.meldinger

import no.nav.dagpenger.mottak.Hendelse

class MinsteinntektVurderingData(
    private val journalpostId: String,
    private val oppfyllerMinsteArbeidsinntekt: Boolean?
) : Hendelse() {
    override fun journalpostId(): String = journalpostId
    fun oppfyllerMinsteArbeidsinntekt(): Boolean? = oppfyllerMinsteArbeidsinntekt
}
