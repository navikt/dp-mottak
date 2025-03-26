package no.nav.dagpenger.mottak.tjenester

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
import io.ktor.client.request.HttpRequestData
import io.ktor.http.HttpHeaders
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.tjenester.SaksbehandlingHttpKlient.Companion.httpClient
import org.junit.jupiter.api.Test
import java.util.UUID

class SaksbehandlingHttpKlientTest {
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
                httpClient = httpClient(mockEngine),
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
}
