package no.nav.dagpenger.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository

internal fun Route.journalpostRoute(repository: InnsendingMetadataRepository) {
    get("v1/journalpost/{soknadId}") {
        call.parameters["soknadId"]?.let { soknadId ->

            call.respondText("Journalpost for soknadId $soknadId", status = HttpStatusCode.OK)
        }
    }
}
