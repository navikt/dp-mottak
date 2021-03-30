package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Hendelse

class PersonInformasjon(
    private val journalpostId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val norskTilknytning: Boolean,
    private val diskresjonskode: String? = null
) : Hendelse() {
    override fun journalpostId(): String = journalpostId
}
