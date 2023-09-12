package no.nav.dagpenger.mottak

abstract class Hendelse protected constructor(
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {

    abstract fun journalpostId(): String

    init {
        aktivitetslogg.kontekst(this)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            SpesifikkKontekst(it, mapOf("journalpostId" to journalpostId()))
        }
    }

    fun toLogString() = aktivitetslogg.toString()
}
