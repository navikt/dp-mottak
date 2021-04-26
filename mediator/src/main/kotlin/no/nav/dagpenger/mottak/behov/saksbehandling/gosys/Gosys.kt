package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.Config.tokenProvider
import java.time.LocalDate

internal interface GosysOppslag {
    suspend fun opprettOppgave(oppgave: GosysOppgaveRequest): String
}

internal data class GosysOppgaveRequest(
    val journalpostId: String,
    val aktørId: String?,
    val tildeltEnhetsnr: String,
    val aktivDato: LocalDate,
    val fristFerdigstillelse: LocalDate = aktivDato,
    val prioritet: String = "NORM",
    val beskrivelse: String = "Kunne ikke automatisk journalføres"
) {
    val oppgavetype: String = "JFR"
    val opprettetAvEnhetsnr: String = "9999"
    val tema: String = "DAG"
}

private data class GosysOppgaveReponse(val id: String)

internal class GosysProxyClient(config: Configuration) : GosysOppslag {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val tokenProvider = config.tokenProvider

    private val proxyGosysClient = HttpClient() {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/proxy/v1/oppgaver")
            method = HttpMethod.Post
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
            }
        }
    }

    override suspend fun opprettOppgave(oppgaveRequest: GosysOppgaveRequest): String {
        try {
            logger.info { "Forsøker å opprette oppgave i gosys for sak med journalpostId ${oppgaveRequest.journalpostId}" }
            return proxyGosysClient.request<GosysOppgaveReponse> {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                body = oppgaveRequest
            }.id
        } catch (e: Exception) {
            logger.info { "Kunne ikke opprette oppgave i gosys for sak med journalpostId ${oppgaveRequest.journalpostId}" }
            throw e
        }
    }
}
