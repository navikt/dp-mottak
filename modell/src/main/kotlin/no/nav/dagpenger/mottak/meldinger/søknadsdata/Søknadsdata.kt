package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.node.JsonNodeFactory
import com.fasterxml.jackson.databind.node.NullNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor

class Søknadsdata(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val data: JsonNode
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId
    fun søknad(): RutingOppslag {
        return rutingOppslag(data)
    }
}

internal fun rutingOppslag(data: JsonNode): RutingOppslag {
    return when (erQuizSøknad(data)) {
        true -> QuizSøknadFormat(data)
        else -> NullSøknadData
    }
}

private fun erQuizSøknad(data: JsonNode) = data["versjon_navn"]?.let {
    !it.isNull && it.asText() == "Dagpenger"
}

object NullSøknadData : RutingOppslag {
    override fun data(): JsonNode = JsonNodeFactory.instance.objectNode()
    override fun accept(visitor: SøknadVisitor) {
        visitor.visitSøknad(this)
    }
    override fun eøsBostedsland() = false
    override fun eøsArbeidsforhold() = false
    override fun avtjentVerneplikt() = false
    override fun avsluttetArbeidsforhold() = emptyList<AvsluttetArbeidsforhold>()
    override fun permittertFraFiskeForedling() = false
    override fun avsluttetArbeidsforholdFraKonkurs() = false
    override fun permittert() = false
}

fun main() {
    println("${NullNode.instance}")
    NullSøknadData.data().toString().let { println(it) }
}
