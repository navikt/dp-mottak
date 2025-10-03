package no.nav.dagpenger.mottak.behov.saksbehandling

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.behov.JsonMapper
import java.time.LocalDateTime
import java.util.UUID

internal interface OppgaveKlient {
    suspend fun opprettOppgave(
        fagsakId: UUID,
        journalpostId: String,
        opprettetTidspunkt: LocalDateTime,
        ident: String,
        skjemaKategori: String,
    ): UUID
}

class OppgaveHttpKlient(
    engine: HttpClientEngine = CIO.create(),
    dpSaksbehandlingBaseUrl: String = Config.dpSaksbehandlingBaseUrl,
    private val tokenProvider: () -> String,
) : OppgaveKlient {
    private val client =
        HttpClient(engine) {
            expectSuccess = true
            install(DefaultRequest) {
                url(urlString = "$dpSaksbehandlingBaseUrl/klage/opprett")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
            }
        }

    override suspend fun opprettOppgave(
        fagsakId: UUID,
        journalpostId: String,
        opprettetTidspunkt: LocalDateTime,
        ident: String,
        skjemaKategori: String,
    ): UUID {
        return client.post {
            this.setBody(
                Request(
                    opprettet = opprettetTidspunkt,
                    journalpostId = journalpostId,
                    sakId = fagsakId,
                    personIdent = Request.PersonIdent(ident),
                ),
            )
        }.body<Response>().oppgaveId
    }

    private data class Response(
        val oppgaveId: UUID,
    )

    private data class Request(
        val opprettet: LocalDateTime,
        val journalpostId: String,
        val sakId: UUID,
        val personIdent: PersonIdent,
    ) {
        data class PersonIdent(
            val ident: String,
        )
    }
}
