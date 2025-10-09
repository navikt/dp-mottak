package no.nav.dagpenger.mottak.behov.saksbehandling

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.behov.JsonMapper
import java.time.LocalDateTime
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

interface SaksbehandlingKlient {
    suspend fun skalVarsleOmEttersending(
        søknadId: String,
        ident: String,
    ): Boolean

    suspend fun opprettOppgave(
        fagsakId: UUID,
        journalpostId: String,
        opprettetTidspunkt: LocalDateTime,
        ident: String,
    ): UUID

    suspend fun hentSisteSakId(ident: String): SisteSakIdResult
}

class SaksbehandlingHttpKlient(
    private val dpSaksbehandlingBaseUrl: String = Config.dpSaksbehandlingBaseUrl,
    private val tokenProvider: () -> String = Config.dpSaksbehandlingTokenProvider,
    engine: HttpClientEngine = CIO.create(),
) : SaksbehandlingKlient {
    private val httpClient =
        HttpClient(engine) {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
            }
            install(DefaultRequest) {
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
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
    ): UUID {
        return httpClient.post(urlString = "$dpSaksbehandlingBaseUrl/klage/opprett") {
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            setBody(
                OpprettOppgaveRequest(
                    opprettet = opprettetTidspunkt,
                    journalpostId = journalpostId,
                    sakId = fagsakId,
                    personIdent = OpprettOppgaveRequest.PersonIdent(ident),
                ),
            )
        }.body<OpprettOppgaveResponse>().oppgaveId
    }

    override suspend fun hentSisteSakId(ident: String): SisteSakIdResult {
        return httpClient.post(urlString = "$dpSaksbehandlingBaseUrl/sak/siste-sak-id/for-ident") {
            header(HttpHeaders.Accept, ContentType.Application.Json)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                mapOf("ident" to ident),
            )
        }.let { httpResponse ->
            when (httpResponse.status) {
                HttpStatusCode.OK -> {
                    httpResponse.body<SisteSakIdResult.Funnet>()
                }

                HttpStatusCode.NoContent -> {
                    SisteSakIdResult.IkkeFunnet
                }

                else -> throw RuntimeException("Uventet svar fra dp-saksbehandling: ${httpResponse.status}")
            }
        }
    }

    override suspend fun skalVarsleOmEttersending(
        søknadId: String,
        ident: String,
    ): Boolean {
        return httpClient.post(urlString = "$dpSaksbehandlingBaseUrl/person/skal-varsle-om-ettersending") {
            header(HttpHeaders.Accept, ContentType.Text.Plain)
            header(HttpHeaders.ContentType, ContentType.Application.Json)
            setBody(
                SoknadDTO(
                    ident = ident,
                    soknadId = UUID.fromString(søknadId),
                ),
            )
        }.bodyAsText().let {
            when (it) {
                "true" -> true
                "false" -> false
                else -> throw RuntimeException("Uventet svar fra dp-saksbehandling: $it")
            }
        }
    }
}

private data class SoknadDTO(
    val ident: String,
    val soknadId: UUID,
)

private data class OpprettOppgaveRequest(
    val opprettet: LocalDateTime,
    val journalpostId: String,
    val sakId: UUID,
    val personIdent: PersonIdent,
) {
    data class PersonIdent(
        val ident: String,
    )
}

private data class OpprettOppgaveResponse(
    val oppgaveId: UUID,
)

sealed class SisteSakIdResult {
    data class Funnet(val id: UUID) : SisteSakIdResult()

    data object IkkeFunnet : SisteSakIdResult()
}
