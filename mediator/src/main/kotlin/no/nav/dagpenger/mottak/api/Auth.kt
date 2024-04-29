package no.nav.dagpenger.mottak.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import com.fasterxml.jackson.databind.DeserializationFeature
import io.ktor.client.HttpClient
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.get
import io.ktor.serialization.jackson.jackson
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.jwt.JWTConfigureFunction
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import no.nav.dagpenger.mottak.Config
import java.net.URI
import java.util.concurrent.TimeUnit

internal fun AuthenticationContext.fnr(): String =
    principal<JWTPrincipal>()?.subject ?: throw IllegalArgumentException("Fant ikke subject(fødselsnummer) i JWT")

internal fun AuthenticationConfig.jwt(
    name: String,
    configure: JWTConfigureFunction = {},
) {
    jwt(name) {
        verifier(
            jwkProvider = cachedJwkProvider(Config.AzureAd.jwksURI),
            issuer = Config.AzureAd.issuer,
            configure,
        )
        validate {
            JWTPrincipal(it.payload)
        }
    }
}

private fun cachedJwkProvider(jwksUri: String): JwkProvider {
    return JwkProviderBuilder(URI(jwksUri).toURL())
        .cached(10, 24, TimeUnit.HOURS) // cache up to 10 JWKs for 24 hours
        .rateLimited(
            10,
            1,
            TimeUnit.MINUTES,
        ) // if not cached, only allow max 10 different keys per minute to be fetched from external provider
        .build()
}

private val httpClient =
    HttpClient {
        install(ContentNegotiation) {
            jackson {
                configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
        }
    }
