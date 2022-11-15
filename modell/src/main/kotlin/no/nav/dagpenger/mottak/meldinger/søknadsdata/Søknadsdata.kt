package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.RutingOppslag

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
        else -> GammeltSøknadFormat(data)
    }
}

private fun erQuizSøknad(data: JsonNode) = data["versjon_navn"]?.let {
    !it.isNull && it.asText() == "Dagpenger"
}
