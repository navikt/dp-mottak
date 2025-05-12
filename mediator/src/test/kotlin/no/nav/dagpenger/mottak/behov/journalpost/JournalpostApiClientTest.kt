package no.nav.dagpenger.mottak.behov.journalpost

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.assertions.json.shouldEqualSpecifiedJson
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.URLProtocol
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
                "/rest/journalpostapi/v1/journalpost/123",
                requestData.url.encodedPathAndQuery,
            )
            assertEquals(URLProtocol.HTTPS, requestData.url.protocol)
            assertEquals("dokarkiv.dev-fss-pub.nais.io", requestData.url.host)
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
                "/rest/journalpostapi/v1/journalpost/123/ferdigstill",
                requestData.url.encodedPathAndQuery,
            )
            assertEquals("dokarkiv.dev-fss-pub.nais.io", requestData.url.host)

            assertEquals("Bearer token", requestData.headers["Authorization"])
            assertEquals("eksternReferanseId", requestData.headers["X-Request-ID"])
        }

    @Test
    fun `skal kunne knytte en journalpost til en annen fagsak`() {
        runBlocking {
            val gammelJournalpostId = "123"
            val nyJournalpostId = "456"
            val mockHttpEngine =
                MockEngine { request ->
                    respond(
                        //language=json
                        content = """{"nyJournalpostId": "$nyJournalpostId"}""",
                        status = HttpStatusCode.OK,
                        headers = headersOf(HttpHeaders.ContentType, "application/json"),
                    )
                }
            val journalpostApi = JournalpostApiClient(engine = mockHttpEngine, { "token" })
            journalpostApi.knyttJounalPostTilNySak(
                journalpostId = gammelJournalpostId,
                dagpengerFagsakId = "fagsakId",
                ident = "ident",
            ) shouldEqualJson
                //language=json
                """
                {"nyJournalpostId": "$nyJournalpostId"}
                """.trimIndent()

            mockHttpEngine.requestHistory.single().let { request ->
                request.method.value shouldBe "PUT"
                request.url.encodedPathAndQuery shouldBe "/rest/journalpostapi/v1/journalpost/$gammelJournalpostId/knyttTilAnnenSak"
                request.headers["Authorization"] shouldBe "Bearer token"
                request.body.toByteArray().decodeToString() shouldEqualSpecifiedJson
                    //language=json
                    """
                    {
                      "fagsakId": "fagsakId",
                      "bruker": {
                        "id": "ident",
                        "idType": "FNR"
                      },
                      "sakstype": "FAGSAK",
                      "fagsaksystem": "DAGPENGER",
                      "tema": "DAG",
                      "journalfoerendeEnhet": "9999"
                    }
                    """.trimIndent()
            }
        }
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
