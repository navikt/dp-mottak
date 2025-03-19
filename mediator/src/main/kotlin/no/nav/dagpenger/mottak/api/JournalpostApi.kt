package no.nav.dagpenger.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.application.Application
import io.ktor.server.auth.authenticate
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.db.InnsendingRepository

internal fun Application.journalpostApi(

) {
    routing {
        authenticate(Config.AzureAd.NAME) {
            get("v1/journalpost/{soknadId}") {
                call.parameters["soknadId"]?.let { soknadId ->
                    call.respondText("Journalpost for soknadId $soknadId", status = HttpStatusCode.OK)
                }
            }
        }
    }}