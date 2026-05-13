package no.nav.dagpenger.mottak.api

import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson3.JacksonConverter
import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.callid.CallId
import io.ktor.server.plugins.callid.callIdMdc
import io.ktor.server.plugins.calllogging.CallLogging
import io.ktor.server.plugins.calllogging.processingTimeMillis
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.document
import io.ktor.server.request.httpMethod
import io.ktor.server.request.path
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.routing
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.Config.objectMapper
import no.nav.dagpenger.mottak.api.models.HttpProblemDTO
import java.net.URI
import java.util.UUID

fun Application.installPlugins(routeSetup: Route.() -> Unit) {
    install(ContentNegotiation) {
        register(ContentType.Application.Json, JacksonConverter(objectMapper))
    }

    install(CallId) {
        header("callId")
        verify { it.isNotEmpty() }
        generate { UUID.randomUUID().toString() }
    }

    install(CallLogging) {
        disableDefaultColors()
        callIdMdc("callId")
        filter { call ->
            !setOf("isalive", "isready", "metrics").contains(call.request.document())
        }
        format { call ->
            val status = call.response.status()?.value ?: "Unhandled"
            val method = call.request.httpMethod.value
            val path = call.request.path()
            val duration = call.processingTimeMillis()
            "$status $method $path $duration ms"
        }
    }

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is io.ktor.server.plugins.BadRequestException,
                is IllegalArgumentException,
                -> {
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
