package no.nav.dagpenger.mottak

import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

interface IBehovslogg {
    fun behov(type: Behovtype, melding: String, detaljer: Map<String, Any> = emptyMap())
    fun kontekst(kontekst: Behovskontekst)
    fun behov(): List<Behov>
}

class Behovslogg : IBehovslogg {

    private val behov = mutableListOf<Behov>()
    private val kontekster = mutableListOf<Behovskontekst>()

    override fun behov(type: Behovtype, melding: String, detaljer: Map<String, Any>) {
        behov.add(Behov(type, kontekster.toSpesifikk(), melding, detaljer))
    }

    override fun behov(): List<Behov> = behov.toList()

    override fun kontekst(kontekst: Behovskontekst) {
        kontekster.add(kontekst)
    }
}

private fun MutableList<Behovskontekst>.toSpesifikk() = this.map { it.toSpesifikkKontekst() }

class Behov(
    val type: Behovtype,
    kontekster: List<SpesifikkKontekst>,
    private val melding: String,
    private val detaljer: Map<String, Any> = emptyMap(),
    private val tidsstempel: String = LocalDateTime.now().format(tidsstempelformat)
) {
    private companion object {
        private val tidsstempelformat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS")
    }
}

interface Behovskontekst {
    fun toSpesifikkKontekst(): SpesifikkKontekst
}

class SpesifikkKontekst(internal val kontekstType: String, internal val kontekstMap: Map<String, String> = mapOf()) {
    internal fun melding() =
        kontekstType + kontekstMap.entries.joinToString(separator = ", ", prefix = " - ") { "${it.key}: ${it.value}" }

    override fun equals(other: Any?) =
        this === other || other is SpesifikkKontekst && this.kontekstMap == other.kontekstMap

    override fun hashCode() = kontekstMap.hashCode()
}

enum class Behovtype {
    Journalpost,
    Persondata
}
