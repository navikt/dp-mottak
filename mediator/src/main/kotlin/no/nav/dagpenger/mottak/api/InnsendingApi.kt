package no.nav.dagpenger.mottak.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.callloging.CallLogging
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.request.document
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.ReplayFerdigstillEvent
import no.nav.dagpenger.mottak.db.InnsendingRepository
import java.time.LocalDateTime

private val logger = KotlinLogging.logger { }

internal fun Application.innsendingApi(
    innsendingRepository: InnsendingRepository,
    observer: InnsendingObserver,
) {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, cause.message ?: "Feil!")
                else -> {
                    logger.error(cause) { "Kunne ikke håndtere API kall" }
                    call.respond(
                        HttpStatusCode.InternalServerError,
                    )
                }
            }
        }
    }
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }

    install(CallLogging) {
        disableDefaultColors()
        filter { call ->
            !setOf(
                "isalive",
                "isready",
                "metrics",
            ).contains(call.request.document())
        }
    }

    install(Authentication) {
        jwt(Config.AzureAd.NAME) {
            withAudience(Config.AzureAd.audience)
        }
    }
    routing {
        authenticate(Config.AzureAd.NAME) {
            put("/internal/replay/{journalpostId}") {
                val journalpostId =
                    this.call.parameters["journalpostId"]
                        ?: throw IllegalArgumentException("Må sette parameter til journalpostid")
                val innsending =
                    innsendingRepository.hent(journalpostId)?.also {
                        it.addObserver(observer)
                    } ?: throw IllegalArgumentException("Fant ikke journalpostId med id $journalpostId")
                innsending.håndter(ReplayFerdigstillEvent(journalpostId))
                call.respond("OK")
            }
            get("/innsending/periode") {
                logger.info { "Skal hente innsendingsperiode med:\n ${this.call.parameters}" }
                val periode = Periode(this.call.parameters["fom"]!!, this.call.parameters["tom"]!!)
                val innsendinger = innsendingRepository.forPeriode(periode)
                call.respond(innsendinger)
            }
        }
    }
}

data class Periode(val fom: LocalDateTime, val tom: LocalDateTime) {
    init {
        require(fom.isBefore(tom)) { " FOM kan ikke være etter TOM " }
    }

    constructor(fom: String, tom: String) : this(LocalDateTime.parse(fom), LocalDateTime.parse(tom))
}
