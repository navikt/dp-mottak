package no.nav.dagpenger.mottak.behov.saksbehandling.arena

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.features.ClientRequestException
import io.ktor.client.features.DefaultRequest
import io.ktor.client.features.HttpTimeout
import io.ktor.client.features.json.JacksonSerializer
import io.ktor.client.features.json.JsonFeature
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.statement.readText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyTokenProvider
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.behov.JsonMapper
import java.time.Duration
import java.time.LocalDate

internal interface ArenaOppslag {
    suspend fun harEksisterendeSaker(fnr: String): Boolean
    suspend fun opprettStartVedtakOppgave(journalpostId: String, parametere: OpprettArenaOppgaveParametere): OpprettVedtakOppgaveResponse?
    suspend fun opprettVurderHenvendelsOppgave(journalpostId: String, parametere: OpprettArenaOppgaveParametere): OpprettVedtakOppgaveResponse?
}

internal class ArenaApiClient(config: Configuration) : ArenaOppslag {

    companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    private val tokenProvider = config.dpProxyTokenProvider

    private val baseUrl = "${config.dpProxyUrl()}/proxy/v1/arena"
    private val proxyArenaClient = HttpClient(engine = CIO.create { requestTimeout = Long.MAX_VALUE }) {
        install(DefaultRequest) {
            method = HttpMethod.Post
        }
        install(HttpTimeout) {
            connectTimeoutMillis = Duration.ofSeconds(30).toMillis()
            requestTimeoutMillis = Duration.ofSeconds(30).toMillis()
            socketTimeoutMillis = Duration.ofSeconds(30).toMillis()
        }
        install(JsonFeature) {
            serializer = JacksonSerializer(jackson = JsonMapper.jacksonJsonAdapter)
        }
    }

    override suspend fun harEksisterendeSaker(fnr: String): Boolean {
        sikkerlogg.info { "Forsøker å hente eksisterende saker fra arena for fnr $fnr" }
        return proxyArenaClient.request<AktivSakResponse>("$baseUrl/sak/aktiv") {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            body = AktivSakRequest(fnr)
        }.harAktivSak
    }

    override suspend fun opprettStartVedtakOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere
    ): OpprettVedtakOppgaveResponse? = opprettArenaOppgave("$baseUrl/vedtak", parametere)

    private suspend fun opprettArenaOppgave(url: String, parametereBody: OpprettArenaOppgaveParametere): OpprettVedtakOppgaveResponse? =
        try {
            proxyArenaClient.request(url) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                body = parametereBody
            }
        } catch (e: ClientRequestException) {
            val message = e.response.readText()
            if (e.response.status.value == 400) {
                logger.warn { "Kunne ikke opprette Arena oppgave, feilmelding: $message" }
                null
            } else {
                throw e
            }
        }

    override suspend fun opprettVurderHenvendelsOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere
    ): OpprettVedtakOppgaveResponse? = opprettArenaOppgave("$baseUrl/sak/henvendelse", parametere)
}

private data class AktivSakRequest(val fnr: String)
private data class AktivSakResponse(val harAktivSak: Boolean)

internal data class OpprettArenaOppgaveParametere(
    val naturligIdent: String,
    val behandlendeEnhetId: String,
    val tilleggsinformasjon: String,
    val registrertDato: LocalDate,
    val oppgavebeskrivelse: String
)

internal data class OpprettVedtakOppgaveResponse(
    val fagsakId: String?,
    val oppgaveId: String
)
