package no.nav.dagpenger.mottak.behov.vilkårtester

import com.natpryce.konfig.Configuration
import io.ktor.client.HttpClient
import io.ktor.client.features.DefaultRequest
import io.ktor.client.request.header
import io.ktor.client.request.request
import io.ktor.client.request.url
import io.ktor.http.HttpMethod
import mu.KotlinLogging
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import no.nav.dagpenger.mottak.Config.dpProxyScope
import no.nav.dagpenger.mottak.Config.dpProxyUrl
import no.nav.dagpenger.mottak.behov.JsonMapper.jacksonJsonAdapter
import java.time.LocalDate

internal interface RegelApiClient {
    suspend fun startMinsteinntektVurdering(aktørId: String, journalpostId: String)
}

internal class RegelApiProxy(config: Configuration) : RegelApiClient {

    companion object {
        private val logger = KotlinLogging.logger {}
    }

    private val tokenProvider = ClientCredentialsClient(config) {
        scope {
            add(config.dpProxyScope())
        }
    }
    private val proxyBehovClient = HttpClient {
        install(DefaultRequest) {
            this.url("${config.dpProxyUrl()}/proxy/v1/regelapi/behov")
            method = HttpMethod.Post
            header("Content-Type", "application/json")
        }
    }

    override suspend fun startMinsteinntektVurdering(aktørId: String, journalpostId: String) {
        proxyBehovClient.request<String> {
            body = BehovRequest(
                aktorId = aktørId,
                regelkontekst = RegelKontekst(id = journalpostId),
                beregningsdato = LocalDate.now()
            ).toJson()
        }.also {
            logger.info { "Opprettet minsteinntekt vurdering behov for journapost med id $journalpostId, status: $it" }
        }
    }
}

internal data class BehovRequest(
    val aktorId: String,
    val regelkontekst: RegelKontekst,
    val beregningsdato: LocalDate,
    val regelverksdato: LocalDate? = beregningsdato
) {
    fun toJson(): String = jacksonJsonAdapter.writeValueAsString(this)
}

internal data class RegelKontekst(val id: String, private val type: String = "soknad")
