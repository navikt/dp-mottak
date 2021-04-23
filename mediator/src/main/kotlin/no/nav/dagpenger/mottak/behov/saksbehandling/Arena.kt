package no.nav.dagpenger.mottak.behov.saksbehandling

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

interface ArenaOppslag {
    suspend fun harEksisterendeSaker(fnr: String): Boolean
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
}

private data class AktivSakRequest(val fnr: String)
private data class AktivSakResponse(val harAktivSak: Boolean)
