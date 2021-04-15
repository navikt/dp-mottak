package no.nav.dagpenger.mottak.proxy

import com.natpryce.konfig.Configuration
import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.response.respondText
import io.ktor.routing.get
import io.ktor.routing.routing
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import no.nav.dagpenger.mottak.Config.dpProxyScope
import no.nav.dagpenger.mottak.Config.dpProxyUrl

internal fun proxyPing(configuration: Configuration): Application.() -> Unit {
    val tokenProvider = ClientCredentialsClient(configuration) {
        scope {
            add(configuration.dpProxyScope())
        }
    }
    val proxyPingClient = HttpClient() {
        install(DefaultRequest) {
            this.url("${configuration.dpProxyUrl()}/proxy/v1/ping") // endre til riktig url
            method = HttpMethod.Get
        }
    }
    return {
        routing {
            get("/internal/proxyping") {
                proxyPingClient.request<String>() {
                    header(HttpHeaders.Authorization, "Bearer ${tokenProvider.getAccessToken()}") // ! Viktig; sett opp
                }.let {
                    call.respondText(it)
                }
            }
        }
    }
}
