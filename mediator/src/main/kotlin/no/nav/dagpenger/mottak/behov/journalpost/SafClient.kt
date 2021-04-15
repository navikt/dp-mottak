package no.nav.dagpenger.mottak.behov.journalpost

import com.fasterxml.jackson.annotation.JsonIgnore
import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import no.nav.dagpenger.mottak.Configuration.dpProxyScope
import no.nav.dagpenger.mottak.Configuration.dpProxyUrl
import no.nav.dagpenger.mottak.Søknad.Companion.fromJson
import no.nav.dagpenger.mottak.behov.GraphqlQuery

internal interface JournalpostArkiv {
    suspend fun hentJournalpost(journalpostId: String): Saf.Journalpost
}

internal interface SøknadsArkiv {
    suspend fun hentSøknadsData(journalpostId: String, dokumentInfoId: String): Saf.SøknadsData
}

internal data class JournalPostQuery(@JsonIgnore val journalpostId: String) : GraphqlQuery(
    //language=Graphql
    query =
    """ 
            query(${'$'}journalpostId: String!) {
                journalpost(journalpostId: ${'$'}journalpostId) {
                    journalstatus
                    journalpostId
                    journalfoerendeEnhet
                    datoOpprettet
                    behandlingstema
                    bruker {
                      type
                      id
                    }
                    relevanteDatoer {
                      dato
                      datotype
                    }
                    dokumenter {
                      tittel
                      dokumentInfoId
                      brevkode
                    }
                }
            }
    """.trimIndent(),
    variables = mapOf(
        "journalpostId" to journalpostId
    )
)

internal class SafClient(private val config: Configuration) : JournalpostArkiv, SøknadsArkiv {
    private val tokenProvider = ClientCredentialsClient(config) {
        scope {
            add(config.dpProxyScope())
        }
    }
    private val proxyJoarkClient = HttpClient {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/proxy/v1/saf/graphql")
            method = HttpMethod.Post
        }
    }

    private val proxySøknadsDataClient = HttpClient {
        install(DefaultRequest) {
            method = HttpMethod.Get
        }
    }

    override suspend fun hentJournalpost(journalpostId: String): Saf.Journalpost =
        proxyJoarkClient.request<String> {
            header("Content-Type", "application/json")
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            body = JournalPostQuery(journalpostId).toJson()
        }.let {
            Saf.Journalpost.fromGraphQlJson(it)
        }

    override suspend fun hentSøknadsData(journalpostId: String, dokumentInfoId: String): Saf.SøknadsData =
        proxySøknadsDataClient.request<String>("${config.dpProxyUrl()}/proxy/v1/saf/rest/hentdokument/$journalpostId/$dokumentInfoId/ORIGINAL") {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
        }.let {
            Saf.SøknadsData.fromJson(it)
        }
}


fun main() {

}