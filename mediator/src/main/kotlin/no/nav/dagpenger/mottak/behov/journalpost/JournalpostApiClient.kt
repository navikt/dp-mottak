package no.nav.dagpenger.mottak.behov.journalpost

import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import io.ktor.client.HttpClient
import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.ClientRequestException
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logging
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.URLProtocol
import io.ktor.http.encodedPath
import io.ktor.serialization.jackson.JacksonConverter
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.behov.JsonMapper
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.Bruker
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.SaksType
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.SaksType.FAGSAK
import kotlin.time.Duration.Companion.minutes

internal class JournalpostApiClient(
    engine: HttpClientEngine = CIO.create(),
    private val tokenProvider: () -> String,
    private val basePath: String = "rest/journalpostapi/v1",
) : JournalpostDokarkiv {
    private companion object {
        val logger = KotlinLogging.logger { }
    }

    private val client =
        HttpClient(engine) {
            expectSuccess = true
            install(HttpTimeout) {
                requestTimeoutMillis = 2.minutes.inWholeMilliseconds
            }
            install(Logging) {
                level = LogLevel.INFO
            }
            install(DefaultRequest) {
                header("X-Nav-Consumer", "dp-mottak")
                url {
                    protocol = URLProtocol.HTTPS
                    host = Config.properties[Key("DOKARKIV_INGRESS", stringType)]
                }
            }
            install(ContentNegotiation) {
                register(ContentType.Application.Json, JacksonConverter(JsonMapper.jacksonJsonAdapter))
            }
        }

    override suspend fun oppdaterJournalpost(
        journalpostId: String,
        journalpost: JournalpostApi.OppdaterJournalpostRequest,
        eksternReferanseId: String,
    ) {
        val feilmelding = "Kunne ikke oppdatere journalpost"
        try {
            client.put {
                url { encodedPath = "$basePath/journalpost/$journalpostId" }
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.XRequestId, eksternReferanseId)
                header(HttpHeaders.ContentType, "application/json")
                header(HttpHeaders.Accept, "application/json")
                setBody(journalpost)
            }.also {
                logger.info { "Oppdaterte journalpost $journalpostId" }
            }
        } catch (e: ClientRequestException) {
            logger.error(e) { feilmelding }
            throw JournalpostFeil.JournalpostException(
                e.response.status.value,
                e.response.bodyAsText(),
            )
        } catch (e: Throwable) {
            logger.error(e) { feilmelding }
            throw e
        }
    }

    override suspend fun ferdigstill(
        journalpostId: String,
        eksternReferanseId: String,
    ) {
        val feilmelding = "Kunne ikke ferdigstille journalpost"
        try {
            client.patch {
                url { encodedPath = "$basePath/journalpost/$journalpostId/ferdigstill" }
                header(HttpHeaders.XRequestId, eksternReferanseId)
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
                header(HttpHeaders.ContentType, "application/json")
                setBody(FerdigstillJournalpostRequest())
            }
        } catch (e: ClientRequestException) {
            logger.error(e) { feilmelding }
            throw JournalpostFeil.JournalpostException(
                e.response.status.value,
                e.response.bodyAsText(),
            )
        } catch (e: Throwable) {
            logger.error(e) { feilmelding }
            throw e
        }
    }

    override suspend fun knyttJounalPostTilNySak(
        journalpostId: String,
        dagpengerFagsakId: String,
        ident: String,
    ): String {
        return client.put("/$basePath/journalpost/$journalpostId/knyttTilAnnenSak") {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            header(HttpHeaders.ContentType, "application/json")
            header(HttpHeaders.Accept, "application/json")
            setBody(
                KnyttTilAnnenSakRequest(
                    fagsakId = dagpengerFagsakId,
                    bruker = Bruker(id = ident),
                ),
            )
        }.bodyAsText()
    }

    private data class KnyttTilAnnenSakRequest(
        val fagsakId: String,
        val bruker: Bruker,
    ) {
        val sakstype: SaksType = FAGSAK
        val fagsaksystem: String = "DAGPENGER"
        val tema: String = "DAG"
        val journalfoerendeEnhet: String = "9999"
    }

    private data class FerdigstillJournalpostRequest(val journalfoerendeEnhet: String = "9999")
}
