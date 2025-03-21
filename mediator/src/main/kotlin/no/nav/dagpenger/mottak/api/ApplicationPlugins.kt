package no.nav.dagpenger.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.statuspages.StatusPagesConfig
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.api.models.HttpProblemDTO
import java.net.URI

fun StatusPagesConfig.statusPages() {
    exception<Throwable> { call, cause ->
        when (cause) {
            is IllegalArgumentException -> {
                val httpProblemDTO =
                    HttpProblemDTO(
                        title = "Ugyldig verdi",
                        status = HttpStatusCode.BadRequest.value,
                        detail = cause.message,
                        instance = call.request.path(),
                        type = URI.create("dagpenger.nav.no/mottak:problem:ugyldig-verdi").toString(),
                    )
                call.respond(HttpStatusCode.BadRequest, httpProblemDTO)
            }
            else -> {
                call.respond(
                    HttpStatusCode.InternalServerError,
                    HttpProblemDTO(
                        title = "Uhåndtert feil",
                        status = HttpStatusCode.InternalServerError.value,
                        detail = cause.message,
                        instance = call.request.path(),
                        type = URI.create("dagpenger.nav.no/mottak:problem:uhåndtert-feil").toString(),
                    ),
                )
            }
        }
    }
}

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
