package no.nav.dagpenger.mottak.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.server.application.Application
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.TestApplicationCall
import io.ktor.server.testing.TestApplicationEngine
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
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
        mockOAuth2Server.issueToken(
            issuerId = Config.AzureAd.NAME,
            claims =
                mapOf(
                    "aud" to Config.AzureAd.audience,
                ),
        ).serialize()
    }

    internal fun withMockAuthServerAndTestApplication(
        moduleFunction: Application.() -> Unit,
        test: suspend ApplicationTestBuilder.() -> Unit,
    ) {
        try {
            System.setProperty("AZURE_OPENID_CONFIG_JWKS_URI", "${mockOAuth2Server.jwksUrl(Config.AzureAd.NAME)}")
            System.setProperty("AZURE_OPENID_CONFIG_ISSUER", "${mockOAuth2Server.issuerUrl(Config.AzureAd.NAME)}")
            testApplication {
                application(moduleFunction)
                test()
            }
        } finally {
        }
    }

    internal fun HttpRequestBuilder.autentisert(token: String = azureAd) {
        this.header(HttpHeaders.Authorization, "Bearer $token")
    }

    internal fun TestApplicationEngine.autentisert(
        endepunkt: String,
        token: String = azureAd,
        httpMethod: HttpMethod = HttpMethod.Get,
        setup: TestApplicationRequest.() -> Unit = {},
    ): TestApplicationCall =
        handleRequest(httpMethod, endepunkt) {
            addHeader(HttpHeaders.Authorization, "Bearer $token")
            setup()
        }
}
