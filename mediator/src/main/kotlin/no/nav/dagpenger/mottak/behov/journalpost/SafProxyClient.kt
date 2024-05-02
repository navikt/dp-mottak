package no.nav.dagpenger.mottak.behov.journalpost

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyTokenProvider
import no.nav.dagpenger.mottak.Config.dpProxyUrl

internal class SafProxyClient(config: Configuration) : JournalpostArkiv, SøknadsArkiv {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val tokenProvider = config.dpProxyTokenProvider
    private val dpProxyUrl = config.dpProxyUrl()

    private val proxyJoarkClient =
        HttpClient {
            expectSuccess = true
        }

    private val proxySøknadsDataClient =
        HttpClient {
            expectSuccess = true
        }

    override suspend fun hentJournalpost(journalpostId: String): SafGraphQL.Journalpost =
        proxyJoarkClient.request("$dpProxyUrl//proxy/v1/saf/graphql") {
            header("Content-Type", "application/json")
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            method = HttpMethod.Post
            setBody(JournalPostQuery(journalpostId).toJson())
        }.let {
            SafGraphQL.Journalpost.fromGraphQlJson(it.bodyAsText())
        }

    override suspend fun hentSøknadsData(
        journalpostId: String,
        dokumentInfoId: String,
    ): SafGraphQL.SøknadsData =
        try {
            proxySøknadsDataClient.request("$dpProxyUrl/proxy/v1/saf/rest/hentdokument/$journalpostId/$dokumentInfoId/ORIGINAL") {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                method = HttpMethod.Get
            }.let {
                SafGraphQL.SøknadsData.fromJson(it.bodyAsText())
            }
        } catch (exception: ClientRequestException) {
            if (exception.response.status == HttpStatusCode.NotFound) {
                logger.warn(exception) { "Fant ikke dokumentInfo for journalpostId $journalpostId med dokumentinfoId $dokumentInfoId" }
                SafGraphQL.SøknadsData.fromJson("{}")
            } else {
                throw exception
            }
        }
}
