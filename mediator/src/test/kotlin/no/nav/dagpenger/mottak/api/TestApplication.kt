package no.nav.dagpenger.mottak.api

import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.routing.Route
import io.ktor.server.testing.testApplication
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
            testApplication {
                application {
                    installPlugins {
                        moduleFunction()
                    }
                }
                val testClient =
                    createClient {
                        install(ContentNegotiation) {
                            register(ContentType.Application.Json, JacksonConverter(Config.objectMapper))
                        }
                    }
                test(TestContext(testClient))
            }
        } finally {
        }
    }

    internal fun HttpRequestBuilder.autentisert(token: String = azureAd) {
        this.header(HttpHeaders.Authorization, "Bearer $token")
    }
}

internal class TestContext(val client: HttpClient)
