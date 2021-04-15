package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class Søknadsdata(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val data: JsonNode
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun søknad(): Søknad = Søknad(data)

    data class Søknad(
        val data: JsonNode
    ) {
        fun søknadsId(): String? = data["brukerBehandlingId"].textValue()
    }
}
