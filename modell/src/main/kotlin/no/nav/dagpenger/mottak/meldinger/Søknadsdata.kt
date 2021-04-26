package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.SøknadFakta
import no.nav.dagpenger.mottak.SøknadVisitor

class Søknadsdata(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val data: JsonNode
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun søknad(): Søknad = Søknad(data)

    class Søknad(
        val data: JsonNode
    ) : SøknadFakta {
        fun søknadsId(): String? = data["brukerBehandlingId"].textValue()

        override fun getFakta(faktaNavn: String): List<JsonNode> =
            data.get("fakta")?.filter { it["key"].asText() == faktaNavn } ?: emptyList()

        override fun getBooleanFaktum(faktaNavn: String) = getFaktumValue(
            getFakta(faktaNavn)
        ).asBoolean()

        override fun getBooleanFaktum(faktaNavn: String, defaultValue: Boolean) = kotlin.runCatching {
            getFaktumValue(
                getFakta(faktaNavn)
            ).asBoolean()
        }.getOrDefault(defaultValue)

        override fun getChildFakta(faktumId: Int): List<JsonNode> =
            data.get("fakta").filter { it["parrentFaktum"].asInt() == faktumId }

        private fun getFaktumValue(fakta: List<JsonNode>): JsonNode = fakta
            .first()
            .get("value")

        fun accept(visitor: SøknadVisitor) {
            visitor.visitSøknad(this)
        }
    }
}
