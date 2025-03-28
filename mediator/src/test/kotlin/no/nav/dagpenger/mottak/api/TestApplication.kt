package no.nav.dagpenger.mottak.api

import com.github.navikt.tbd_libs.naisful.test.TestContext
import com.github.navikt.tbd_libs.naisful.test.naisfulTestApp
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.server.routing.Route
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import no.nav.dagpenger.mottak.Config
import no.nav.security.mock.oauth2.MockOAuth2Server

internal object TestApplication {
    private val mockOAuth2Server: MockOAuth2Server by lazy {
        MockOAuth2Server().also { server ->
            server.start()
        }
    }

    internal val azureAd: String by lazy {
        mockOAuth2Server
            .issueToken(
                issuerId = Config.AzureAd.NAME,
                claims =
                    mapOf(
                        "aud" to Config.AzureAd.audience,
                    ),
            ).serialize()
    }

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Route.() -> Unit,
        test: suspend TestContext.() -> Unit,
    ) {
        try {
            System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "${mockOAuth2Server.jwksUrl(Config.AzureAd.NAME)}")
            System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "${mockOAuth2Server.issuerUrl(Config.AzureAd.NAME)}")
            return naisfulTestApp(
                {
                    this.installPlugins {
                        moduleFunction()
                    }
                },
                Config.objectMapper,
                PrometheusMeterRegistry(PrometheusConfig.DEFAULT),
            ) {
                test()
            }
        } finally {
        }
    }

    internal fun HttpRequestBuilder.autentisert(token: String = azureAd) {
        this.header(HttpHeaders.Authorization, "Bearer $token")
    }
}
