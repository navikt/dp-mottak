package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.PersonVisitor

class PersonInformasjon(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val aktørId: String,
    private val fødselsnummer: String,
    private val norskTilknytning: Boolean,
    private val diskresjonskode: String? = null
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun person(): Person = Person(aktørId, fødselsnummer, norskTilknytning, harDiskresjonkode(diskresjonskode))

    private fun harDiskresjonkode(diskresjonskode: String?): Boolean =
        when (diskresjonskode) {
            "STRENGT_FORTROLIG_UTLAND", "STRENGT_FORTROLIG" -> true
            else -> false
        }

    data class Person(
        val aktørId: String,
        val fødselsnummer: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: Boolean
    ) {
        fun accept(visitor: PersonVisitor) {
            visitor.visitPerson(aktørId, fødselsnummer, norskTilknytning, diskresjonskode)
        }
    }
}

class PersonInformasjonIkkeFunnet(aktivitetslogg: Aktivitetslogg, private val journalpostId: String) :
    Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
