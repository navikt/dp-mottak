package no.nav.dagpenger.mottak.behov.saksbehandling.arena

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.apache5.Apache5
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.HttpTimeout
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
import java.time.Duration
import java.time.LocalDate

internal interface ArenaOppslag {
    suspend fun opprettStartVedtakOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere,
    ): OpprettVedtakOppgaveResponse?

    suspend fun opprettVurderHenvendelsOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere,
    ): OpprettVedtakOppgaveResponse?
}

internal class ArenaApiClient(
    config: Configuration,
) : ArenaOppslag {
    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.ArenaApiClient")
    }

    private val tokenProvider = config.dpProxyTokenProvider

    private val baseUrl = "${config.dpProxyUrl()}/proxy/v1/arena"
    private val proxyArenaClient =
        HttpClient(engine = Apache5.create { connectionRequestTimeout = Long.MAX_VALUE }) {
            expectSuccess = true
            install(HttpTimeout) {
                connectTimeoutMillis = Duration.ofSeconds(30).toMillis()
                requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
                socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
            }
        }

    override suspend fun opprettStartVedtakOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere,
    ): OpprettVedtakOppgaveResponse? = opprettArenaOppgave("$baseUrl/vedtak", parametere)

    private suspend fun opprettArenaOppgave(
        url: String,
        parametereBody: OpprettArenaOppgaveParametere,
    ): OpprettVedtakOppgaveResponse? =
        try {
            proxyArenaClient
                .request(url) {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                    header(HttpHeaders.ContentType, "application/json")
                    header(HttpHeaders.Accept, "application/json")
                    method = HttpMethod.Post
                    setBody(parametereBody)
                }.body()
        } catch (e: ClientRequestException) {
            val message = e.response.bodyAsText()
            if (e.response.status.value == 400) {
                logger.warn { "Kunne ikke opprette Arena oppgave, feilmelding: $message" }
                null
            } else {
                throw e
            }
        }

    override suspend fun opprettVurderHenvendelsOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere,
    ): OpprettVedtakOppgaveResponse? = opprettArenaOppgave("$baseUrl/sak/henvendelse", parametere)
}

private data class AktivSakRequest(
    val fnr: String,
)

private data class AktivSakResponse(
    val harAktivSak: Boolean,
)

internal data class OpprettArenaOppgaveParametere(
    val naturligIdent: String,
    val behandlendeEnhetId: String,
    val tilleggsinformasjon: String,
    val registrertDato: LocalDate,
    val oppgavebeskrivelse: String,
)

internal data class OpprettVedtakOppgaveResponse(
    val fagsakId: String?,
    val oppgaveId: String,
)
