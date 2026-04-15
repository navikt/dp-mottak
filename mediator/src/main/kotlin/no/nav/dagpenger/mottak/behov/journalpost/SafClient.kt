package no.nav.dagpenger.mottak.behov.journalpost

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import no.nav.dagpenger.mottak.Config.safTokenProvider
import no.nav.dagpenger.mottak.Config.safUrl

internal class SafClient(config: Configuration) : JournalpostArkiv {
    private val tokenProvider = config.safTokenProvider
    private val safUrl = config.safUrl()

    private val joarkClient =
        HttpClient {
            expectSuccess = true
        }

    override suspend fun hentJournalpost(journalpostId: String): SafGraphQL.Journalpost =
        joarkClient.request("$safUrl/graphql") {
            header("Content-Type", "application/json")
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            method = HttpMethod.Post
            setBody(JournalPostQuery(journalpostId).toJson())
        }.let {
            SafGraphQL.Journalpost.fromGraphQlJson(it.bodyAsText())
        }
}
