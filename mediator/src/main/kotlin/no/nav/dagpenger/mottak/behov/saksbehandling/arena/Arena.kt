package no.nav.dagpenger.mottak.behov.saksbehandling.arena

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
import java.time.LocalDateTime

interface ArenaOppslag {
    suspend fun harEksisterendeSaker(fnr: String): Boolean
    suspend fun opprettStartVedtakOppgave(
        fødselsnummer: String,
        aktørId: String,
        behandlendeEnhet: String,
        beskrivelse: String,
        tilleggsinformasjon: String,
        registrertDato: LocalDateTime,
        journalpostId:String
    ): Map<String, String>
}

class ArenaApiClient(config: Configuration) : ArenaOppslag {

    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    private val tokenProvider = config.tokenProvider

    private val proxyArenaClient = HttpClient() {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/proxy/v1/arena/sak/aktiv")
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
        return proxyArenaClient.request<AktivSakResponse> {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            body = AktivSakRequest(fnr)
        }.harAktivSak
    }

    override suspend fun opprettStartVedtakOppgave(
        fødselsnummer: String,
        aktørId: String,
        behandlendeEnhet: String,
        beskrivelse: String,
        tilleggsinformasjon: String,
        registrertDato: LocalDateTime,
        journalpostId: String
    ): Map<String, String> {
        sikkerlogg.info { "Forsøker å opprette start vedtak oppgave i Arena" }
        return proxyArenaClient.request<OpprettVedtakOppgaveResponse> {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            body = OpprettVedtakOppgaveRequest(
                naturligIdent = fødselsnummer,
                behandlendeEnhetId = behandlendeEnhet,
                tilleggsinformasjon = tilleggsinformasjon,
                registrertDato = registrertDato.toLocalDate(),
                oppgavebeskrivelse = beskrivelse
            )
        }.map(journalpostId)
    }
}

private data class AktivSakRequest(val fnr: String)
private data class AktivSakResponse(val harAktivSak: Boolean)

private data class OpprettVedtakOppgaveRequest(
    val naturligIdent: String,
    val behandlendeEnhetId: String,
    val tilleggsinformasjon: String,
    val registrertDato: LocalDate,
    val oppgavebeskrivelse: String
)

private data class OpprettVedtakOppgaveResponse(
    val fagsakId: String,
    val oppgaveId:String
){
    fun map(journalpostId: String)= mapOf(
        "fagsakId" to fagsakId,
        "oppgaveId" to oppgaveId,
        "journalpostId" to journalpostId
    )
}