package no.nav.dagpenger.mottak.behov.person

import com.fasterxml.jackson.annotation.JsonIgnore
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.DeserializationContext
import com.fasterxml.jackson.databind.JsonDeserializer
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import mu.KotlinLogging
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import no.nav.dagpenger.mottak.Configuration.dpProxyScope
import no.nav.dagpenger.mottak.Configuration.dpProxyUrl
import no.nav.dagpenger.mottak.behov.GraphqlQuery
import no.nav.dagpenger.mottak.behov.GraphqlQuery.Companion.jacksonJsonAdapter

private val sikkerLogg = KotlinLogging.logger("tjenestekall")

internal class PdlPersondataOppslag(config: Configuration) : PersonOppslag {
    private val tokenProvider = ClientCredentialsClient(config) {
        scope {
            add(config.dpProxyScope())
        }
    }
    private val proxyPdlClient = HttpClient() {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/proxy/v1/pdl/graphql")
            method = HttpMethod.Post
        }
    }

    override suspend fun hentPerson(id: String): Pdl.Person = proxyPdlClient.request<String> {
        header("Content-Type", "application/json")
        header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
        body = PersonQuery(id).toJson().also { sikkerLogg.info { it } }
    }.let {
        Pdl.Person.fromGraphQlJson(it)
    }
}

internal data class PersonQuery(@JsonIgnore val id: String) : GraphqlQuery(
    //language=Graphql
    query =
    """query {
    hentPerson(ident "$id") {
        navn {
            fornavn,
            mellomnavn,
            etternavn
        },
        adressebeskyttelse{
            gradering
        }
    }
    hentGeografiskTilknytning(ident: "$id"){
        gtLand
    }
    hentIdenter(ident: "$id", grupper: [AKTORID,FOLKEREGISTERIDENT]) {
        identer {
            ident,
            gruppe
        }
    }                
}
    """.trimIndent(),
    variables = null
)

internal class Pdl {

    @JsonDeserialize(using = PersonDeserializer::class)
    data class Person(
        val navn: String,
        val aktørId: String,
        val fødselsnummer: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: String?
    ) {
        internal companion object {
            fun fromGraphQlJson(json: String): Person =
                jacksonJsonAdapter.readValue(json, Person::class.java)
        }
    }

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
            // if errors not
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