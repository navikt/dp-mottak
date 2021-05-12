package no.nav.dagpenger.mottak.api

import io.ktor.application.Application
import io.ktor.application.call
import io.ktor.application.install
import io.ktor.auth.Authentication
import io.ktor.auth.UserIdPrincipal
import io.ktor.auth.authenticate
import io.ktor.auth.basic
import io.ktor.features.StatusPages
import io.ktor.http.HttpStatusCode
import io.ktor.response.respond
import io.ktor.routing.put
import io.ktor.routing.routing
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.ReplayFerdigstillEvent
import no.nav.dagpenger.mottak.db.InnsendingRepository

internal fun innsendingApi(
    innsendingRepository: InnsendingRepository,
    observer: InnsendingObserver,
    credential: Pair<String, String>
): Application.() -> Unit {
    val (username, password) = credential
    return {
        install(StatusPages) {
            exception<IllegalArgumentException> { cause ->
                call.respond(HttpStatusCode.BadRequest, cause.message ?: "Feil!")
            }
        }
        install(Authentication) {
            basic {
                realm = "teamdagpenger-access-to-replay"

                validate { credential ->
                    if (credential.name == username && credential.password == password) {
                        UserIdPrincipal(credential.name)
                    } else null
                }
            }
        }
        routing {
            authenticate {
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
        }
    }
}
