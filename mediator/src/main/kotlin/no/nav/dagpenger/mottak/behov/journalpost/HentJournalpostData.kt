package no.nav.dagpenger.mottak.proxy

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
import no.nav.dagpenger.mottak.behov.GraphqlQuery
import no.nav.dagpenger.mottak.behov.journalpost.Saf

internal interface JournalpostArkiv {
    suspend fun hentJournalpost(journalpostId: String): Saf.Journalpost
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

internal class HentJournalpostData(config: Configuration) : JournalpostArkiv {
    private val tokenProvider = ClientCredentialsClient(config) {
        scope {
            add(config.dpProxyScope())
        }
    }
    private val proxyJoarkClient = HttpClient() {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/proxy/v1/saf/graphql")
            method = HttpMethod.Post
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
}
