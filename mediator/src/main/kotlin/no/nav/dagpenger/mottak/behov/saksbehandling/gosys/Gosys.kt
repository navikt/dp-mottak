package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.JacksonConverter
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyTokenProvider
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.behov.JsonMapper
import java.time.LocalDate

internal interface GosysOppslag {
    suspend fun opprettOppgave(oppgave: GosysOppgaveRequest): String
}

internal data class GosysOppgaveRequest(
    val journalpostId: String,
    val aktoerId: String?,
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

internal data class GosysOppgaveResponse(val id: String)

internal class GosysProxyClient(config: Configuration) : GosysOppslag {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val tokenProvider = config.dpProxyTokenProvider

    private val proxyGosysClient = HttpClient() {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/proxy/v1/gosys/oppgaver")
//            method = HttpMethod.Post
        }
        install(ContentNegotiation) {
            register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
        }
    }

    override suspend fun opprettOppgave(oppgave: GosysOppgaveRequest): String {
        return try {
            logger.info { "Forsøker å opprette oppgave i gosys for sak med journalpostId ${oppgave.journalpostId}" }
            proxyGosysClient.request {
                header("X-Correlation-ID", oppgave.journalpostId)
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                method = HttpMethod.Post
                setBody(oppgave)
            }.body<GosysOppgaveResponse>().id
        } catch (e: Exception) {
            logger.info { "Kunne ikke opprette oppgave i gosys for sak med journalpostId ${oppgave.journalpostId}" }
            throw e
        }
    }
}
