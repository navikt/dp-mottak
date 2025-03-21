package no.nav.dagpenger.mottak.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.ContentType
import io.ktor.http.HttpStatusCode
import io.ktor.http.contentType
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.api.TestApplication.autentisert
import no.nav.dagpenger.mottak.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.util.UUID

class JournalpostApiTest {
    private val søknadId = UUID.randomUUID()
    private val testIdent = "12345"

    @Test
    fun `Endepunkt for henting av journalpost krever autentisering`() {
        withMockAuthServerAndTestApplication({ journalpostRoute(mockk()) }) {
            client.post("v1/journalpost/sok") {
                contentType(ContentType.Application.Json)
                setBody("""{"soknadId": "$søknadId", "ident": "$testIdent"}""")
            }.status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Disabled
    @Test
    fun `Skal svare med HttpProblem ved feil input og interne feil`() {
        val søknadId = "MikkeMus"

        withMockAuthServerAndTestApplication({ journalpostRoute(mockk()) }) {
            client.post("v1/journalpost/sok") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody("""{"soknadId": "$søknadId", "ident": "$testIdent"}""")
            }.let { response ->
                response.status shouldBe HttpStatusCode.BadRequest
                response.bodyAsText() shouldEqualJson
                    //language=json
                    """
                    {
                        "journalpostIder": ["123456789" , "987654321"]
                    }
                    """.trimMargin()
            }
        }
    }

    @Test
    fun `Skal kunne hente journalposter for en søknadId og eventuelle ettersendinger`() {
        val søknadId = UUID.randomUUID()
        val repository =
            mockk<InnsendingMetadataRepository>().also {
                every { it.hentJournalpostIder(søknadId, testIdent) } returns listOf("123456789", "987654321")
            }
        withMockAuthServerAndTestApplication({ journalpostRoute(repository) }) {
            client.post("v1/journalpost/sok") {
                autentisert()
                contentType(ContentType.Application.Json)
                setBody("""{"soknadId": "$søknadId", "ident": "$testIdent"}""")
            }.let { response ->
                response.status shouldBe HttpStatusCode.OK
                response.headers["Content-Type"] shouldBe "application/json; charset=UTF-8"
                response.bodyAsText() shouldEqualJson
                    //language=json
                    """
                    {
                        "journalpostIder": ["123456789" , "987654321"]
                    }
                    """.trimMargin()
            }
        }
    }
}
