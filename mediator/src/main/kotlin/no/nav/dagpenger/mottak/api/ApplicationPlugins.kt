package no.nav.dagpenger.mottak.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mottak.Config

fun Application.installPlugins(routeSetup: Route.() -> Unit) {
    install(Authentication) {
        jwtBuilder(Config.AzureAd.NAME) {
            withAudience(Config.AzureAd.audience)
        }
    }
    routing {
        authenticate(Config.AzureAd.NAME) {
            routeSetup()
        }
    }
}
