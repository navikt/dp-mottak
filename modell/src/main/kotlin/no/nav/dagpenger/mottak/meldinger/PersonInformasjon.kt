package no.nav.dagpenger.mottak.meldinger

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
    private val diskresjonskode: String? = null,
    private val egenAnsatt: Boolean = false,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun person(): Person =
        Person(
            navn = navn,
            aktørId = aktørId,
            ident = ident,
            norskTilknytning = norskTilknytning,
            diskresjonskode = harDiskresjonkode(diskresjonskode),
            egenAnsatt = egenAnsatt,
        )

    fun validate(): Boolean =
        kotlin.runCatching { person() }.fold(
            onSuccess = { true },
            onFailure = {
                this.warn("PersonInformasjon inneholder ugyldig data: ${it.message}")
                false
            },
        )

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
        val diskresjonskode: Boolean,
        val egenAnsatt: Boolean,
    ) {
        init {
            require(ident.matches(Regex("\\d{11}"))) { "Ident må ha 11 siffer" }
        }

        fun accept(visitor: PersonVisitor) {
            visitor.visitPerson(navn, aktørId, ident, norskTilknytning, diskresjonskode, egenAnsatt)
        }
    }
}

class PersonInformasjonIkkeFunnet(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
}
