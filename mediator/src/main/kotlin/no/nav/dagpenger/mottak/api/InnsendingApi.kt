package no.nav.dagpenger.mottak.api

import io.ktor.server.application.Application
import io.ktor.server.application.install
import io.ktor.server.auth.Authentication
import io.ktor.server.auth.authenticate
import io.ktor.server.plugins.BadRequestException
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
    install(Authentication) {
        jwt(Config.AzureAd.NAME) {
            withAudience(Config.AzureAd.audience)
        }
    }
    routing {
        authenticate(Config.AzureAd.NAME) {
            put("/internal/replay/{journalpostId}") {
                try {
                    val journalpostId =
                        this.call.parameters["journalpostId"]
                            ?: throw IllegalArgumentException("Må sette parameter til journalpostid")
                    val innsending =
                        innsendingRepository.hent(journalpostId)?.also {
                            it.addObserver(observer)
                        } ?: throw IllegalArgumentException("Fant ikke journalpostId med id $journalpostId")
                    innsending.håndter(ReplayFerdigstillEvent(journalpostId))
                    call.respond("OK")
                } catch (e: IllegalArgumentException) {
                    throw BadRequestException(e.message ?: "Ukjent feil")
                }
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

data class Periode(
    val fom: LocalDateTime,
    val tom: LocalDateTime,
) {
    init {
        require(fom.isBefore(tom)) { " FOM kan ikke være etter TOM " }
    }

    constructor(fom: String, tom: String) : this(LocalDateTime.parse(fom), LocalDateTime.parse(tom))
}
