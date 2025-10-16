package no.nav.dagpenger.mottak.tjenester

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.http.fullPath
import io.ktor.http.headersOf
import io.ktor.utils.io.ByteReadChannel
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.behov.saksbehandling.SakIdResponse
import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingHttpKlient
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class SaksbehandlingHttpKlientTest {
    @Test
    fun `henting av siste sakId`() {
        val personMedSak = "personMedSak"
        val personUtenSak = "personUtenSak"
        var requestData: HttpRequestData? = null
        val sisteSakId = UUID.randomUUID()

        val mockEngine =
            MockEngine { request ->
                requestData = request
                val bodyAsText = request.body.toByteArray().decodeToString()
                when (bodyAsText.contains(personMedSak)) {
                    true -> {
                        respond(""" {"id": "$sisteSakId"} """, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    }

                    false -> {
                        respond(content = "", status = HttpStatusCode.NoContent)
                    }
                }
            }
        val saksbehandlingHttpKlient =
            SaksbehandlingHttpKlient(
                engine = mockEngine,
                tokenProvider = { "token" },
            )

        runBlocking {
            saksbehandlingHttpKlient.hentSisteSakId(personMedSak) shouldBe SakIdResponse.Funnet(sisteSakId)
            saksbehandlingHttpKlient.hentSisteSakId(personUtenSak) shouldBe SakIdResponse.IkkeFunnet
        }

        requireNotNull(requestData).let { request ->
            request.method.value shouldBe "POST"
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
            request.url.toString() shouldBe "http://dp-saksbehandling/sak/siste-sak-id/for-ident"
        }
    }

    @Test
    fun `henting av sakId for søknad`() {
        val søknadMedSak = UUID.randomUUID()
        val søknadUtenSak = UUID.randomUUID()
        var requestData: HttpRequestData? = null
        val sakId = UUID.randomUUID()

        val mockEngine =
            MockEngine { request ->
                requestData = request
                val harSak = request.url.fullPath.contains(søknadMedSak.toString())
                when (harSak) {
                    true -> {
                        respond(""" {"id": "$sakId"} """, status = HttpStatusCode.OK, headers = headersOf(HttpHeaders.ContentType, "application/json"))
                    }

                    false -> {
                        respond(content = "", status = HttpStatusCode.NoContent)
                    }
                }
            }
        val saksbehandlingHttpKlient =
            SaksbehandlingHttpKlient(
                engine = mockEngine,
                tokenProvider = { "token" },
            )

        runBlocking {
            saksbehandlingHttpKlient.hentSakIdForSøknad(søknadMedSak) shouldBe SakIdResponse.Funnet(sakId)
            assertRequest(requestData = requestData, expectedUrl = "http://dp-saksbehandling/sak/sak-id-for-soknad/$søknadMedSak", expectedMethod = HttpMethod.Get)

            saksbehandlingHttpKlient.hentSakIdForSøknad(søknadUtenSak) shouldBe SakIdResponse.IkkeFunnet
            assertRequest(requestData = requestData, expectedUrl = "http://dp-saksbehandling/sak/sak-id-for-soknad/$søknadUtenSak", expectedMethod = HttpMethod.Get)
        }
    }

    @Test
    fun `Skal kalle dp-saksbehandling for å finne ut om varsel skal sendes`() {
        var requestData: HttpRequestData? = null
        val søknadId = UUID.randomUUID().toString()
        val ident = "12345678910"
        val mockEngine =
            MockEngine { request ->
                requestData = request
                respond("true")
            }
        val saksbehandlingHttpKlient =
            SaksbehandlingHttpKlient(
                engine = mockEngine,
                tokenProvider = { "token" },
            )

        runBlocking {
            saksbehandlingHttpKlient.skalVarsleOmEttersending(
                søknadId = søknadId,
                ident = ident,
            ) shouldBe true
            requireNotNull(requestData).let { request ->
                request.method.value shouldBe "POST"
                request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
                request.url.toString() shouldBe "http://dp-saksbehandling/person/skal-varsle-om-ettersending"
                String(request.body.toByteArray()) shouldEqualJson
                    //language=json
                    """
                    {
                        "soknadId": "$søknadId",
                        "ident": "$ident"
                    }
                """
            }
        }
    }

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

        val saksbehandlingHttpKlient =
            SaksbehandlingHttpKlient(
                engine = mockHttpEngine,
                dpSaksbehandlingBaseUrl = "http://localhost",
                tokenProvider = { "dummyToken" },
            )
        runBlocking {
            saksbehandlingHttpKlient.opprettOppgave(
                fagsakId = fagsakId,
                journalpostId = journalpostId,
                opprettetTidspunkt = opprettetTidspunkt,
                ident = ident,
            ) shouldBe oppgaveId
        }

        requireNotNull(httpRequestData) { "Feil ved oppretting av oppgave" }.let { request ->
            request.method shouldBe HttpMethod.Post
            request.url.toString() shouldBe "http://localhost/klage/opprett"
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer dummyToken"
        }
    }

    private fun assertRequest(
        requestData: HttpRequestData?,
        expectedUrl: String,
        expectedMethod: HttpMethod,
    ) {
        requireNotNull(requestData).let { request ->
            request.method shouldBe expectedMethod
            request.url.toString() shouldBe expectedUrl
            request.headers[HttpHeaders.Authorization] shouldBe "Bearer token"
        }
    }
}
