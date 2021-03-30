package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class PersonInformasjon(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val aktoerId: String,
    private val naturligIdent: String,
    private val norskTilknytning: Boolean,
    private val diskresjonskode: String? = null
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun person(): Person = Person(aktoerId, naturligIdent, norskTilknytning, diskresjonskode)

    data class Person(
        val aktoerId: String,
        val naturligIdent: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: String?
    )
}
