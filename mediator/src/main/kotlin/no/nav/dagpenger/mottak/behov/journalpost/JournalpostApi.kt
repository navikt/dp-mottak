package no.nav.dagpenger.mottak.behov.journalpost

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.Config.tokenProvider

internal interface JournalpostOppdatering {
    suspend fun oppdaterJournalpost(journalpostId: String, journalpost: JournalpostApi.OppdaterJournalpostRequest)
}

internal class JournalpostApi {
    internal data class OppdaterJournalpostRequest(
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

    internal data class Bruker(val id: String, val idType: String = "FNR")
    internal data class Dokument(val dokumentInfoId: String, val tittel: String)
    internal enum class SaksType {
        GENERELL_SAK,
        FAGSAK
    }
}

internal class JournalpostApiClient(private val config: Configuration) : JournalpostOppdatering {

    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    private val journalføringBaseUrl = "${config.dpProxyUrl()}/proxy/v1/dokarkiv/rest/journalpostapi/v1/journalpost"
    private val tokenProvider = config.tokenProvider
    private val proxyJournalpostApiClient = HttpClient() {

        install(DefaultRequest) {
            method = HttpMethod.Put
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
        proxyJournalpostApiClient.request<OppdaterJournalpostResponse>("$journalføringBaseUrl/$journalpostId") {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            body = journalpost
        }
    }
    private data class OppdaterJournalpostResponse(val journalpostId: String)
}
