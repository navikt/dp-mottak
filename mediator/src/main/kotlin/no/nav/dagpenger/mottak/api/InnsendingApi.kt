package no.nav.dagpenger.mottak.api

import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.jackson
import io.ktor.server.application.Application
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.basic
import io.ktor.server.plugins.contentnegotiation.ContentNegotiation
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.routing.get
import io.ktor.server.routing.put
import io.ktor.server.routing.routing
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.ReplayFerdigstillEvent
import no.nav.dagpenger.mottak.db.InnsendingRepository
import java.time.LocalDateTime

internal fun Application.innsendingApi(
    innsendingRepository: InnsendingRepository,
    observer: InnsendingObserver,
    credential: Pair<String, String>
) {
    val (username, password) = credential

    install(StatusPages) {
        exception<Throwable> { call, cause ->
            when (cause) {
                is IllegalArgumentException -> call.respond(HttpStatusCode.BadRequest, cause.message ?: "Feil!")
            }
        }
    }
    install(ContentNegotiation) {
        jackson {
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            registerModule(JavaTimeModule())
        }
    }

    install(Authentication) {
        basic("basic") {
            realm = "teamdagpenger-access-to-replay"

            validate { credential ->
                if (credential.name == username && credential.password == password) {
                    UserIdPrincipal(credential.name)
                } else null
            }
        }
        jwt(Config.AzureAd.name) {
            withAudience(Config.AzureAd.audience)
        }
    }
    routing {
        authenticate("basic") {
            put("/internal/replay/{journalpostId}") {
                val journalpostId = this.call.parameters["journalpostId"]
                    ?: throw IllegalArgumentException("Må sette parameter til journalpostid")
                val innsending = innsendingRepository.hent(journalpostId)?.also {
                    it.addObserver(observer)
                } ?: throw IllegalArgumentException("Fant ikke journalpostId med id $journalpostId")
                innsending.håndter(ReplayFerdigstillEvent(journalpostId))
                call.respond("OK")
            }
        }
        // authenticate(Config.AzureAd.name) {
        get("/innsending/periode") {
            val periode = Periode(this.call.parameters["fom"]!!, this.call.parameters["tom"]!!)
            val innsendinger = innsendingRepository.forPeriode(periode)
            call.respond(innsendinger)
        }
        // }
    }
}

data class Periode(val fom: LocalDateTime, val tom: LocalDateTime) {

    init {
        require(tom.isBefore(fom)) { " FOM kan ikke være etter TOM " }
    }

    constructor(fom: String, tom: String) : this(LocalDateTime.parse(tom), LocalDateTime.parse(fom))
}
