package no.nav.dagpenger.mottak.meldinger

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
    fun søknad(): RutingOppslag = when {
        data["seksjoner"] != null -> QuizSøknadFormat(data)
        data["fakta"] != null -> GammeltSøknadFormat(data)
        else -> throw IllegalArgumentException("Kunne ikke avgjøre søknadsformat: $data")
    }
}
