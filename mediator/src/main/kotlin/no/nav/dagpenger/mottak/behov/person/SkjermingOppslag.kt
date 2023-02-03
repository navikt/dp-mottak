package no.nav.dagpenger.mottak.behov.person

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.engine.cio.CIO
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import no.nav.dagpenger.mottak.Config.skjermingApiTokenProvider
import no.nav.dagpenger.mottak.Config.skjermingApiUrl

internal class SkjermingOppslag(config: Configuration) {
    private val tokenProvider = config.skjermingApiTokenProvider
    private val httpClient by lazy {
        HttpClient(CIO) {
            expectSuccess = true
            install(DefaultRequest) {
                this.url(config.skjermingApiUrl())
                header("Content-Type", "application/json")
                header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            }
        }
    }

    suspend fun egenAnsatt(id: String): Result<Boolean> {
        return kotlin.runCatching {
            httpClient.post {
                this.setBody("""{"personident":"$id"}""")
            }.bodyAsText().toBoolean()
        }
    }
}
