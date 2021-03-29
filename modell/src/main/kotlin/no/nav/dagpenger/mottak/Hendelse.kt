package no.nav.dagpenger.mottak

abstract class Hendelse protected constructor(
    internal val behovlogg: Behovslogg = Behovslogg()
) : IBehovslogg by behovlogg, Behovskontekst {
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
