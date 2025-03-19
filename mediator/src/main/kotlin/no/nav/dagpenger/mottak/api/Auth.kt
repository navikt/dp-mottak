package no.nav.dagpenger.mottak.api

import com.auth0.jwk.JwkProvider
import com.auth0.jwk.JwkProviderBuilder
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.AuthenticationConfig
import io.ktor.server.auth.AuthenticationContext
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.jwt.JWTConfigureFunction
import io.ktor.server.auth.jwt.JWTPrincipal
import io.ktor.server.auth.jwt.jwt
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mottak.Config
import java.net.URI
import java.util.concurrent.TimeUnit

fun Application.authenticatedRoutes(block: Route.() -> Unit) {
    install(Authentication) {
        jwtBuilder(Config.AzureAd.NAME) {
            withAudience(Config.AzureAd.audience)
        }
    }
    routing {
        authenticate(Config.AzureAd.NAME) {
            block()
        }
    }
}

internal fun AuthenticationContext.fnr(): String =
    principal<JWTPrincipal>()?.subject ?: throw IllegalArgumentException("Fant ikke subject(fødselsnummer) i JWT")

internal fun AuthenticationConfig.jwtBuilder(
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
