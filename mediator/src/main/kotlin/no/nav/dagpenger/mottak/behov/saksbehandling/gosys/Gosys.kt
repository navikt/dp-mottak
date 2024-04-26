package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.serialization.jackson.JacksonConverter
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpGosysTokenProvider
import no.nav.dagpenger.mottak.Config.dpProxyTokenProvider
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.Config.gosysUrl
import no.nav.dagpenger.mottak.behov.JsonMapper
import no.nav.dagpenger.mottak.unleash
import java.time.LocalDate
import kotlin.time.Duration.Companion.minutes

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
    val beskrivelse: String = "Kunne ikke automatisk journalføres",
) {
    val oppgavetype: String = "JFR"
    val opprettetAvEnhetsnr: String = "9999"
    val tema: String = "DAG"
}

internal data class GosysOppgaveResponse(val id: String)

internal class UnleashGosysClient(config: Configuration) : GosysOppslag {
    private val gosysClient = GosysClient(config)
    private val proxyClient = GosysProxyClient(config)

    override suspend fun opprettOppgave(oppgave: GosysOppgaveRequest): String {
        return if (unleash.isEnabled("bruk-gosys-direkte")) {
            gosysClient.opprettOppgave(oppgave)
        } else {
            proxyClient.opprettOppgave(oppgave)
        }
    }
}

internal class GosysProxyClient(config: Configuration) : GosysOppslag {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val tokenProvider = config.dpProxyTokenProvider

    private val proxyGosysClient =
        HttpClient {
            expectSuccess = true
            install(DefaultRequest) {
                this.url("${config.dpProxyUrl()}/proxy/v1/gosys/oppgaver")
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
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
            logger.error { "Kunne ikke opprette gosys oppgave for journalpost med id ${oppgave.journalpostId}" }
            throw e
        }
    }
}

internal class GosysClient(config: Configuration) : GosysOppslag {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val tokenProvider = config.dpGosysTokenProvider

    private val gosysClient =
        HttpClient {
            expectSuccess = true
            install(DefaultRequest) {
                this.url("${config.gosysUrl()}/api/v1/oppgaver")
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
            }
            install(HttpTimeout) {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
            }
        }

    override suspend fun opprettOppgave(oppgave: GosysOppgaveRequest): String {
        return try {
            logger.info { "Forsøker å opprette oppgave i gosys for sak med journalpostId ${oppgave.journalpostId}" }
            gosysClient.request {
                header("X-Correlation-ID", oppgave.journalpostId)
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                method = HttpMethod.Post
                setBody(oppgave)
            }.body<GosysOppgaveResponse>().id
        } catch (e: Exception) {
            logger.error { "Kunne ikke opprette gosys oppgave for journalpost med id ${oppgave.journalpostId}" }
            throw e
        }
    }
}
