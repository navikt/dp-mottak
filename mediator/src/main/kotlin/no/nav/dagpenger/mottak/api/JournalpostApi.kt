package no.nav.dagpenger.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import no.nav.dagpenger.mottak.api.models.JournalpostIderDTO
import no.nav.dagpenger.mottak.api.models.JournalpostSokDTO
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import java.util.UUID

internal fun Route.journalpostRoute(repository: InnsendingMetadataRepository) {
    post("v1/journalpost/sok") {
        call.receive<JournalpostSokDTO>().let {
            call.respond(
                status = HttpStatusCode.OK,
                message = JournalpostIderDTO(journalpostIder = repository.hentArenaOppgaver(søknadId = UUID.fromString(it.soknadId), ident = it.ident).map { it.journalpostId }),
            )
        }
    }
}
