package no.nav.dagpenger.mottak.behov.saksbehandling

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.parameter
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.Config.tokenProvider
import java.time.LocalDate

interface ArenaOppslag {
    suspend fun harEksisterendeSaker(fnr: String, virkningstidspunkt: LocalDate = LocalDate.now()): Boolean
}

class ArenaApiClient(config: Configuration) : ArenaOppslag {

    companion object {
        private val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    private val tokenProvider = config.tokenProvider

    private val proxyArenaClient = HttpClient() {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/arena/sak/aktiv")
            method = HttpMethod.Get
        }
    }

    override suspend fun harEksisterendeSaker(fnr: String, virkningstidspunkt: LocalDate): Boolean {
        sikkerlogg.info { "Forsøker å hente eksisterende saker fra arena for fnr $fnr" }
        proxyArenaClient.request<String> {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}")
            parameter("fnr", fnr)
            parameter("fom", virkningstidspunkt.minusMonths(36).toString())
            parameter("tom", virkningstidspunkt.toString())
        }.let {
            return it.toBoolean()
        }
    }
}
