package no.nav.dagpenger.mottak.behov.journalpost

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.JacksonConverter
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyTokenProvider
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.behov.JsonMapper

internal interface JournalpostFeil {

    private companion object {
        val logger = KotlinLogging.logger { }
        private val whitelistFeilmeldinger = setOf(
            "Bruker kan ikke oppdateres for journalpost med journalpostStatus=J og journalpostType=I.",
            "er ikke midlertidig journalført",
            "er ikke midlertidig journalf&oslash;rt"
        )
    }

    class JournalpostException(val statusCode: Int, val content: String?) : RuntimeException()

    fun ignorerKjenteTilstander(journalpostException: JournalpostException) {
        when (journalpostException.statusCode) {
            400 -> {
                logger.info { "CONTENT -> ${journalpostException.content}" }
                val json = JsonMapper.jacksonJsonAdapter.readTree(journalpostException.content)

                val feilmelding = json["message"].asText()

                when {
                    feilmelding in whitelistFeilmeldinger -> {
                        return
                    }
                    whitelistFeilmeldinger.any { feilmelding.endsWith(it) } -> {
                        return
                    }
                    else -> {
                        throw journalpostException
                    }
                }
            }
        }
    }
}

internal interface JournalpostDokarkiv {
    suspend fun oppdaterJournalpost(journalpostId: String, journalpost: JournalpostApi.OppdaterJournalpostRequest)
    suspend fun ferdigstill(journalpostId: String)
}

internal class JournalpostApi {
    internal data class OppdaterJournalpostRequest(
        val avsenderMottaker: Avsender,
        val bruker: Bruker,
        val tittel: String,
        val sak: Sak,
        val dokumenter: List<Dokument>,
        val behandlingstema: String = "ab0001",
        val tema: String = "DAG",
        val journalfoerendeEnhet: String = "9999"
    )

    internal data class Sak(val fagsakId: String?) {
        val saksType: SaksType
        val fagsaksystem: String?

        init {
            if (fagsakId != null) {
                saksType = SaksType.FAGSAK
                fagsaksystem = "AO01"
            } else {
                saksType = SaksType.GENERELL_SAK
                fagsaksystem = null
            }
        }
    }

    internal data class Avsender(val navn: String, val id: String, val idType: String = "FNR")

    internal data class Bruker(val id: String, val idType: String = "FNR")
    internal data class Dokument(val dokumentInfoId: String, val tittel: String)
    internal enum class SaksType {
        GENERELL_SAK,
        FAGSAK
    }
}

internal class JournalpostApiClient(config: Configuration) : JournalpostDokarkiv {

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    private val journalføringBaseUrl = "${config.dpProxyUrl()}/proxy/v1/dokarkiv/rest/journalpostapi/v1/journalpost"
    private val tokenProvider = config.dpProxyTokenProvider
    private val proxyJournalpostApiClient = HttpClient() {
        expectSuccess = true
        install(DefaultRequest) {
        }
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
        }
    }

    override suspend fun oppdaterJournalpost(
        journalpostId: String,
        journalpost: JournalpostApi.OppdaterJournalpostRequest
    ) {
        try {
            proxyJournalpostApiClient.request(urlString = "$journalføringBaseUrl/$journalpostId") {
                method = HttpMethod.Put
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                setBody(journalpost)
            }
        } catch (e: ClientRequestException) {
            logger.error(e) { "Kunne ikke oppdatere journalpost" }
            throw JournalpostFeil.JournalpostException(
                e.response.status.value,
                e.response.bodyAsText()
            )
        } catch (e: Throwable) {
            logger.error(e) { "Kunne ikke oppdatere journalpost" }
            throw JournalpostFeil.JournalpostException(
                statusCode = 500,
                "Kunne ikke oppdatere journalpost"
            )
        }
    }

    override suspend fun ferdigstill(journalpostId: String) {
        try {
            proxyJournalpostApiClient.request("$journalføringBaseUrl/$journalpostId/ferdigstill") {
                method = HttpMethod.Patch
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, "application/json")
                setBody(FerdigstillJournalpostRequest())
            }
        } catch (e: ClientRequestException) {
            logger.error(e) { "Kunne ikke ferdigstille journalpost" }
            throw JournalpostFeil.JournalpostException(
                e.response.status.value,
                e.response.bodyAsText()
            )
        } catch (e: Throwable) {
            logger.error(e) { "Kunne ikke oppdatere journalpost" }
            throw JournalpostFeil.JournalpostException(
                statusCode = 500,
                "Kunne ikke oppdatere journalpost"
            )
        }
    }

    private data class FerdigstillJournalpostRequest(val journalfoerendeEnhet: String = "9999")
}
