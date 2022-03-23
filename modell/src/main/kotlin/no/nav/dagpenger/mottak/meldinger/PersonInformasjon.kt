package no.nav.dagpenger.mottak.meldinger

import no.bekk.bekkopen.person.FodselsnummerValidator
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.PersonVisitor

class PersonInformasjon(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val aktørId: String,
    private val ident: String,
    private val norskTilknytning: Boolean,
    private val navn: String,
    private val diskresjonskode: String? = null
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun person(): Person = Person(
        navn = navn,
        aktørId = aktørId,
        ident = ident,
        norskTilknytning = norskTilknytning,
        diskresjonskode = harDiskresjonkode(diskresjonskode)
    )

    fun validate() = kotlin.runCatching { person() }.isSuccess

    private fun harDiskresjonkode(diskresjonskode: String?): Boolean =
        when (diskresjonskode) {
            "STRENGT_FORTROLIG_UTLAND", "STRENGT_FORTROLIG" -> true
            else -> false
        }

    data class Person(
        val navn: String,
        val aktørId: String,
        val ident: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: Boolean
    ) {

        init {
            require(FodselsnummerValidator.isValid(ident) || erSyntetiskTestIdent()) { "Ikke gyldig ident" }
        }

        fun erDnummer() = ident.substring(0, 1).toInt() in 4..7

        fun accept(visitor: PersonVisitor) {
            visitor.visitPerson(navn, aktørId, ident, norskTilknytning, diskresjonskode)
        }

        private fun erSyntetiskTestIdent() = ident[2].toString() == "8"
    }
}

class PersonInformasjonIkkeFunnet(aktivitetslogg: Aktivitetslogg, private val journalpostId: String) :
    Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
