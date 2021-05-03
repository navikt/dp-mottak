package no.nav.dagpenger.mottak.behov.journalpost

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.Config.tokenProvider
import no.nav.dagpenger.mottak.behov.JsonMapper
import java.lang.RuntimeException

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
                if (feilmelding in whitelistFeilmeldinger) {
                    return
                } else throw journalpostException
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
    internal data class Avsender(val navn: String)

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
    private val tokenProvider = config.tokenProvider
    private val proxyJournalpostApiClient = HttpClient() {

        install(DefaultRequest) {
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
            }
        }
    }

    override suspend fun oppdaterJournalpost(
        journalpostId: String,
        journalpost: JournalpostApi.OppdaterJournalpostRequest
    ) {
        try {
            proxyJournalpostApiClient.request<OppdaterJournalpostResponse>("$journalføringBaseUrl/$journalpostId") {
                method = HttpMethod.Put
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                body = journalpost
            }
        } catch (e: ClientRequestException) {
            throw JournalpostFeil.JournalpostException(
                e.response.status.value,
                e.response.readText()
            )
        }
    }

    override suspend fun ferdigstill(journalpostId: String) {
        try {
            proxyJournalpostApiClient.request<String>("$journalføringBaseUrl/$journalpostId/ferdigstill") {
                method = HttpMethod.Patch
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
                header(HttpHeaders.ContentType, "application/json")
                body = FerdigstillJournalpostRequest()
            }
        } catch (e: ClientRequestException) {
            logger.error(e) { "Kunne ikke ferdigstille journalpost" }
            throw JournalpostFeil.JournalpostException(
                e.response.status.value,
                e.response.readText()
            )
        }
    }

    private data class FerdigstillJournalpostRequest(val journalfoerendeEnhet: String = "9999")
    private data class OppdaterJournalpostResponse(val journalpostId: String)
}
