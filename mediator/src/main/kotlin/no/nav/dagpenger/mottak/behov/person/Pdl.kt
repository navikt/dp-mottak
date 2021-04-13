package no.nav.dagpenger.mottak.behov.person

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import mu.KotlinLogging

private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class Pdl {

    @JsonDeserialize(using = PersonDeserializer::class)
    data class Person(
        val navn: String,
        val aktørId: String,
        val fødselsnummer: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: String?
    )

    object PersonDeserializer : JsonDeserializer<Person>() {
        internal fun JsonNode.aktørId() = this.ident("AKTORID")
        internal fun JsonNode.fødselsnummer() = this.ident("FOLKEREGISTERIDENT")
        internal fun JsonNode.norskTilknyting(): Boolean = findValue("gtLand").isNull
        internal fun JsonNode.diskresjonsKode(): String? {
            return findValue("adressebeskyttelse").firstOrNull()?.path("gradering")?.asText(null)
        }

        internal fun JsonNode.personNavn(): String {
            return findValue("navn").first().let { node ->
                val fornavn = node.path("fornavn").asText()
                val mellomnavn = node.path("mellomnavn").asText("")
                val etternavn = node.path("etternavn").asText()

                when (mellomnavn.isEmpty()) {
                    true -> "$fornavn $etternavn"
                    else -> "$fornavn $mellomnavn $etternavn"
                }
            }
        }

        private fun JsonNode.ident(type: String): String {
            return findValue("identer").first { it.path("gruppe").asText() == type }.get("ident").asText()
        }

        override fun deserialize(p: JsonParser, ctxt: DeserializationContext?): Person {
            val node: JsonNode = p.readValueAsTree()
            return kotlin.runCatching {
                Person(
                    navn = node.personNavn(),
                    aktørId = node.aktørId(),
                    fødselsnummer = node.fødselsnummer(),
                    norskTilknytning = node.norskTilknyting(),
                    diskresjonskode = node.diskresjonsKode()
                )
            }.fold(
                onSuccess = {
                    it
                },
                onFailure = {
                    sikkerLogg.info(node.toString())
                    throw it
                }
            )
        }
    }
}
