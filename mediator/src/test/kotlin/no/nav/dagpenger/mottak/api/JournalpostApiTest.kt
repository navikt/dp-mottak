package no.nav.dagpenger.mottak.api

import io.kotest.assertions.json.shouldEqualJson
import io.kotest.matchers.shouldBe
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.api.TestApplication.autentisert
import no.nav.dagpenger.mottak.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import org.junit.jupiter.api.Test
import java.util.UUID

class JournalpostApiTest {
    private val søknadId = UUID.randomUUID()
    private val testIdent = "12345"

    @Test
    fun `Endepunkt for henting av journalpost krever autentisering`() {
        withMockAuthServerAndTestApplication({ journalpostRoute(mockk()) }) {
            client.get("v1/journalpost/$søknadId").status shouldBe HttpStatusCode.Unauthorized
        }
    }

    @Test
    fun `Skal kunen henter journalposter for en søknadId og eventuelle ettersendinger`() {
        val søknadId = UUID.randomUUID()
        val repository =
            mockk<InnsendingMetadataRepository>().also {
                every { it.hentArenaOppgaver(søknadId, testIdent) } returns listOf()
            }
        withMockAuthServerAndTestApplication({ journalpostRoute(repository) }) {
            client.get("v1/journalpost/$søknadId") {
                autentisert()
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
