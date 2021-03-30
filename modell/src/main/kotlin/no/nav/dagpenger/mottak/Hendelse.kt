package no.nav.dagpenger.mottak

abstract class Hendelse protected constructor(
    internal val behovlogg: Aktivitetslogg = Aktivitetslogg()
) : IAktivitetslogg by behovlogg, Aktivitetskontekst {
    abstract fun journalpostId(): String

    init {
        behovlogg.kontekst(this)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            SpesifikkKontekst(it, mapOf("journalpostId" to journalpostId()))
        }
    }
}
