package no.nav.dagpenger.mottak.behov.journalpost

import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import kotlin.test.assertEquals

class JournalpostApiClientTest {
    @Test
    fun `skal oppdatere journalpost`() =
        runBlocking {
            val mockHttpEngine = mockEngine(statusCode = HttpStatusCode.OK)
            val journalpostApi = JournalpostApiClient(engine = mockHttpEngine, { "token" })
            journalpostApi.oppdaterJournalpost(
                "123",
                journalpost,
                "eksternReferanseId",
            )

            val requestData = mockHttpEngine.requestHistory.first()
            assertEquals("PUT", requestData.method.value)
            assertEquals(
                "//dokarkiv.dev-fss-pub.nais.io/rest/journalpostapi/v1/journalpost/123",
                requestData.url.encodedPathAndQuery,
            )
            assertEquals("https://dokarkiv.dev-fss-pub.nais.io", requestData.url.host)
            assertEquals("Bearer token", requestData.headers["Authorization"])
            assertEquals("eksternReferanseId", requestData.headers["X-Request-ID"])
        }

    @Test
    fun `skal kunne ferdigstille journalpost`() =
        runBlocking {
            val mockHttpEngine = mockEngine(statusCode = HttpStatusCode.OK)
            val journalpostApi = JournalpostApiClient(engine = mockHttpEngine, { "token" })
            journalpostApi.ferdigstill(
                "123",
                eksternReferanseId = "eksternReferanseId",
            )

            val requestData = mockHttpEngine.requestHistory.first()
            assertEquals("PATCH", requestData.method.value)
            assertEquals(
                "//dokarkiv.dev-fss-pub.nais.io/rest/journalpostapi/v1/journalpost/123/ferdigstill",
                requestData.url.encodedPathAndQuery,
            )
            assertEquals("https://dokarkiv.dev-fss-pub.nais.io", requestData.url.host)

            assertEquals("Bearer token", requestData.headers["Authorization"])
            assertEquals("eksternReferanseId", requestData.headers["X-Request-ID"])
        }

    @Test
    fun `kaster feil videre hvis en ikke kan oppdatere journalpost`() =
        runBlocking {
            val mockHttpEngine =
                mockEngine(statusCode = HttpStatusCode.BadRequest, content = ByteReadChannel("feilmeling"))

            val journalpostApi = JournalpostApiClient(engine = mockHttpEngine, { "token" })
            val feil =
                assertThrows<JournalpostFeil.JournalpostException> {
                    journalpostApi.oppdaterJournalpost(
                        "123",
                        journalpost,
                        eksternReferanseId = "eksternReferanseId",
                    )
                }
            assertEquals("feilmeling", feil.content)
            assertEquals(HttpStatusCode.BadRequest.value, feil.statusCode)
        }

    private fun mockEngine(
        content: ByteReadChannel = ByteReadChannel("{}"),
        statusCode: HttpStatusCode,
    ) = MockEngine {
        respond(
            content = content,
            status = statusCode,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }

    private val journalpost =
        JournalpostApi.OppdaterJournalpostRequest(
            avsenderMottaker =
                JournalpostApi.Avsender(
                    id = "aptent",
                    idType = "elit",
                ),
            bruker = JournalpostApi.Bruker(id = "eam", idType = "nascetur"),
            tittel = "nonumes",
            sak = JournalpostApi.Sak(fagsakId = "122343"),
            dokumenter =
                listOf(
                    JournalpostApi.Dokument(
                        dokumentInfoId = "omittantur",
                        tittel = "sit",
                    ),
                ),
            behandlingstema = "inani",
            tema = "detracto",
            journalfoerendeEnhet = "alterum",
        )
}
