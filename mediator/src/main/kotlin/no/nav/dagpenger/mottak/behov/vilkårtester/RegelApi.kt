package no.nav.dagpenger.mottak.behov.vilkårtester

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.plugins.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.contentType
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.dpRegelApiTokenProvider
import no.nav.dagpenger.mottak.Config.dpRegelApiUrl
import no.nav.dagpenger.mottak.behov.JsonMapper.jacksonJsonAdapter
import java.time.LocalDate

internal interface RegelApiClient {
    suspend fun startMinsteinntektVurdering(
        aktørId: String,
        journalpostId: String,
    )
}

internal class RegelApiHttpClient(config: Configuration) : RegelApiClient {
    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val tokenProvider = config.dpRegelApiTokenProvider

    private val behovClient =
        HttpClient {
            expectSuccess = true
            install(DefaultRequest) {
                this.url("${config.dpRegelApiUrl()}/behov")
            }
        }

    override suspend fun startMinsteinntektVurdering(
        aktørId: String,
        journalpostId: String,
    ) {
        behovClient.request {
            header(HttpHeaders.Authorization, "Bearer ${tokenProvider.invoke()}")
            contentType(ContentType.Application.Json)
            method = HttpMethod.Post
            setBody(
                BehovRequest(
                    aktorId = aktørId,
                    regelkontekst = RegelKontekst(id = journalpostId),
                    beregningsdato = LocalDate.now().toString(),
                ).toJson(),
            )
        }.also { response ->
            logger.info { "Opprettet minsteinntekt vurdering behov for journalpost med id $journalpostId, status: ${response.status}" }
        }
    }
}

internal data class BehovRequest(
    val aktorId: String,
    val regelkontekst: RegelKontekst,
    val beregningsdato: String,
    val regelverksdato: String? = beregningsdato,
) {
    fun toJson(): String = jacksonJsonAdapter.writeValueAsString(this).trimIndent()
}

internal data class RegelKontekst(val id: String, val type: String = "soknad")
