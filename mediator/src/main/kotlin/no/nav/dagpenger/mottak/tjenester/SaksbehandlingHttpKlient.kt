package no.nav.dagpenger.mottak.tjenester

import io.ktor.client.HttpClient
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
import io.ktor.serialization.jackson.JacksonConverter
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.behov.JsonMapper
import java.util.UUID
import kotlin.time.Duration.Companion.minutes

interface SaksbehandlingKlient {
    suspend fun skalVarsleOmEttersending(
        søknadId: String,
        ident: String,
    ): Boolean
}

class SaksbehandlingHttpKlient(
    private val dpSaksbehandlingBaseUrl: String = Config.dpSaksbehandlingBaseUrl,
    private val tokenProvider: () -> String = Config.dpSaksbehandlingTokenProvider,
    private val httpClient: HttpClient = httpClient(),
) : SaksbehandlingKlient {
    companion object {
        fun httpClient(
            engine: HttpClientEngine = CIO.create(),
        ): HttpClient {
            return HttpClient(engine) {
                expectSuccess = true
                install(HttpTimeout) {
                    requestTimeoutMillis = 2.minutes.inWholeMilliseconds
                }
                install(DefaultRequest) {
                    header(HttpHeaders.Accept, ContentType.Text.Plain)
                    header(HttpHeaders.ContentType, ContentType.Application.Json)
                }
                install(ContentNegotiation) {
                    register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
                }
            }
        }
    }

    override suspend fun skalVarsleOmEttersending(
        søknadId: String,
        ident: String,
    ): Boolean {
        return httpClient.post(urlString = "$dpSaksbehandlingBaseUrl/person/skal-varsle-om-ettersending") {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
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
