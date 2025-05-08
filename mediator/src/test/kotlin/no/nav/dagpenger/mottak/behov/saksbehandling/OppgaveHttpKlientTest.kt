package no.nav.dagpenger.mottak.behov.saksbehandling

import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class OppgaveHttpKlientTest {
    @Test
    fun `skal kunne opprette en klage`() {
        val fagsakId = UUID.randomUUID()
        val journalpostId = "jp"
        val opprettetTidspunkt = LocalDateTime.now()
        val ident = "12345678901"
        val oppgaveId = UUID.randomUUID()

        var httpRequestData: HttpRequestData? = null

        val mockHttpEngine =
            MockEngine { request ->
                httpRequestData = request
                respond(
                    content =
                        ByteReadChannel(
                            """
                            {
                                "oppgaveId": "$oppgaveId"
                            }
                            """.trimIndent(),
                        ),
                    status = HttpStatusCode.Created,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }

        val oppgaveHttpKlient =
            OppgaveHttpKlient(
                engine = mockHttpEngine,
                dpSaksbehandlingBaseUrl = "http://localhost",
                tokenProvider = { "dummyToken" },
            )
        runBlocking {
            oppgaveHttpKlient.opprettOppgave(
                fagsakId = fagsakId,
                journalpostId = journalpostId,
                opprettetTidspunkt = opprettetTidspunkt,
                ident = ident,
                skjemaKategori = "hubba",
            ) shouldBe oppgaveId
        }

        requireNotNull(httpRequestData) { "Feil ved oppretting av oppgave" }.let { request ->
            request.method shouldBe HttpMethod.Post
            request.url.toString() shouldBe "http://localhost/klage/opprett"
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer dummyToken"
        }
    }
}
