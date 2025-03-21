package no.nav.dagpenger.mottak.api

import io.ktor.http.HttpStatusCode
import io.ktor.server.request.receive
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.post
import mu.KotlinLogging
import no.nav.dagpenger.mottak.api.models.JournalpostIderDTO
import no.nav.dagpenger.mottak.api.models.JournalpostSokDTO
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import java.util.UUID

private val sikkerlogg = KotlinLogging.logger("tjenestekall.mottak.journalpostRoute")

internal fun Route.journalpostRoute(repository: InnsendingMetadataRepository) {
    post("v1/journalpost/sok") {
        call.receive<JournalpostSokDTO>().let {
            sikkerlogg.info { "Henter journalpostIder for søknadId: ${it.soknadId} og ident: ${it.ident}" }
            val journalpostIder =
                repository.hentJournalpostIder(
                    søknadId = UUID.fromString(it.soknadId),
                    ident = it.ident,
                )
            call.respond(
                status = HttpStatusCode.OK,
                message = JournalpostIderDTO(journalpostIder = journalpostIder),
            )
        }
    }
}
