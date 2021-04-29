package no.nav.dagpenger.mottak.behov.saksbehandling.arena

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
import java.time.LocalDate

internal interface ArenaOppslag {
    suspend fun harEksisterendeSaker(fnr: String): Boolean
    suspend fun opprettStartVedtakOppgave(journalpostId: String, parametere: OpprettArenaOppgaveParametere): Map<String, String>
    suspend fun opprettVurderHenvendelsOppgave(journalpostId: String, parametere: OpprettArenaOppgaveParametere): Map<String, String>
}

internal class ArenaApiClient(config: Configuration) : ArenaOppslag {

    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    private val tokenProvider = config.tokenProvider

    private val baseUrl = "${config.dpProxyUrl()}/proxy/v1/arena/sak"
    private val proxyArenaClient = HttpClient() {
        install(DefaultRequest) {
            method = HttpMethod.Post
        }
        install(JsonFeature) {
            serializer = JacksonSerializer {
                registerModule(JavaTimeModule())
            }
        }
    }

    override suspend fun harEksisterendeSaker(fnr: String): Boolean {
        sikkerlogg.info { "Forsøker å hente eksisterende saker fra arena for fnr $fnr" }
        return proxyArenaClient.request<AktivSakResponse>("$baseUrl/aktiv") {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            body = AktivSakRequest(fnr)
        }.harAktivSak
    }

    override suspend fun opprettStartVedtakOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere
    ): Map<String, String> = opprettArenaOppgave("$baseUrl/vedtak", parametere).map(journalpostId)

    private suspend fun opprettArenaOppgave(url: String, parametereBody: OpprettArenaOppgaveParametere): OpprettVedtakOppgaveResponse =
        proxyArenaClient.request(url) {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            body = parametereBody
        }

    override suspend fun opprettVurderHenvendelsOppgave(
        journalpostId: String,
        parametere: OpprettArenaOppgaveParametere
    ): Map<String, String> = opprettArenaOppgave("$baseUrl/henvendelse", parametere).map(journalpostId)
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

private data class OpprettVedtakOppgaveResponse(
    val fagsakId: String,
    val oppgaveId: String
) {
    fun map(journalpostId: String) = mapOf(
        "fagsakId" to fagsakId,
        "oppgaveId" to oppgaveId,
        "journalpostId" to journalpostId
    )
}
