package no.nav.dagpenger.mottak

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import no.nav.dagpenger.oauth2.CachedOauth2Client
import no.nav.dagpenger.oauth2.OAuth2Config
import org.apache.kafka.clients.CommonClientConfigs
import org.apache.kafka.common.config.SslConfigs
import org.apache.kafka.common.security.auth.SecurityProtocol
import java.net.InetAddress
import java.net.UnknownHostException
import java.util.Properties

internal object Config {
    private val defaultProperties =
        ConfigurationMap(
            mapOf(
                "DB_DATABASE" to "dp-mottak",
                "DB_HOST" to "localhost",
                "DB_PASSWORD" to "password",
                "DB_PORT" to "5432",
                "DB_USERNAME" to "username",
                "DP_PROXY_SCOPE" to "api://dev-fss.teamdagpenger.dp-proxy/.default",
                "DP_REGEL_API_SCOPE" to "api://dev-gcp.teamdagpenger.dp-regel-api/.default",
                "DP_REGEL_API_URL" to "http://dp-regel-api.teamdagpenger",
                "HTTP_PORT" to "8080",
                "KAFKA_CONSUMER_GROUP_ID" to "dp-mottak-v1",
                "KAFKA_EXTRA_TOPIC" to "teamdagpenger.mottak.v1,teamdagpenger.regel.v1",
                "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
                "KAFKA_RESET_POLICY" to "latest",
                "PDL_API_SCOPE" to "api://dev-fss.pdl.pdl-api/.default",
                "SKJERMING_API_SCOPE" to "api://dev-gcp.nom.skjermede-personer-pip/.default",
                "SKJERMING_API_URL" to "http://skjermede-personer-pip.nom/skjermet",
                "UNLEASH_URL" to "https://unleash.nais.io/api/",
                "AZURE_OPENID_CONFIG_ISSUER" to "azureAd",
                "AZURE_APP_CLIENT_ID" to "azureClientId",
                "AZURE_OPENID_CONFIG_JWKS_URI" to "http://localhost:4443",
                "DOKARKIV_SCOPE" to "api://dev-fss.teamdokumenthandtering.dokarkiv-q1/.default",
                "DOKARKIV_INGRESS" to "dokarkiv.dev-fss-pub.nais.io",
            ),
        )
    private val prodProperties =
        ConfigurationMap(
            mapOf(
                "DOKARKIV_SCOPE" to "api://prod-fss.teamdokumenthandtering.dokarkiv/.default",
                "DOKARKIV_INGRESS" to "dokarkiv.prod-fss-pub.nais.io",
                "DP_PROXY_SCOPE" to "api://prod-fss.teamdagpenger.dp-proxy/.default",
                "DP_REGEL_API_SCOPE" to "api://prod-gcp.teamdagpenger.dp-regel-api/.default",
                "PDL_API_SCOPE" to "api://prod-fss.pdl.pdl-api/.default",
                "SKJERMING_API_SCOPE" to "api://prod-gcp.nom.skjermede-personer-pip/.default",
            ),
        )

    val properties: Configuration by lazy {
        val systemAndEnvProperties = ConfigurationProperties.systemProperties() overriding EnvironmentVariables()
        when (System.getenv().getOrDefault("NAIS_CLUSTER_NAME", "LOCAL")) {
            "prod-gcp" -> systemAndEnvProperties overriding prodProperties overriding defaultProperties
            else -> systemAndEnvProperties overriding defaultProperties
        }
    }

    private val cachedTokenProvider by lazy {
        val azureAd = OAuth2Config.AzureAd(properties)
        CachedOauth2Client(
            tokenEndpointUrl = azureAd.tokenEndpointUrl,
            authType = azureAd.clientSecret(),
        )
    }

    internal fun String.addHttprotocoll(): String = "https://$this"

    val Configuration.dpProxyTokenProvider: () -> String by lazy {
        {
            cachedTokenProvider.clientCredentials(properties[Key("DP_PROXY_SCOPE", stringType)]).accessToken
        }
    }
    val Configuration.dpRegelApiTokenProvider: () -> String by lazy {
        {
            cachedTokenProvider.clientCredentials(properties[Key("DP_REGEL_API_SCOPE", stringType)]).accessToken
        }
    }

    val Configuration.pdlApiTokenProvider: () -> String by lazy {
        {
            cachedTokenProvider.clientCredentials(properties[Key("PDL_API_SCOPE", stringType)]).accessToken
        }
    }
    val Configuration.skjermingApiTokenProvider: () -> String by lazy {
        {
            cachedTokenProvider.clientCredentials(properties[Key("SKJERMING_API_SCOPE", stringType)]).accessToken
        }
    }

    val Configuration.dokarkivTokenProvider: () -> String by lazy {
        {
            cachedTokenProvider.clientCredentials(properties[Key("DOKARKIV_SCOPE", stringType)]).accessToken
        }
    }

    val kafkaProducerProperties: Properties by lazy {
        Properties().apply {
            put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, properties[Key("KAFKA_BROKERS", stringType)])
            put(CommonClientConfigs.SECURITY_PROTOCOL_CONFIG, SecurityProtocol.SSL.name)
            put(SslConfigs.SSL_ENDPOINT_IDENTIFICATION_ALGORITHM_CONFIG, "")
            put(SslConfigs.SSL_TRUSTSTORE_TYPE_CONFIG, "jks")
            put(SslConfigs.SSL_KEYSTORE_TYPE_CONFIG, "PKCS12")
            put(SslConfigs.SSL_TRUSTSTORE_LOCATION_CONFIG, properties[Key("KAFKA_TRUSTSTORE_PATH", stringType)])
            put(SslConfigs.SSL_TRUSTSTORE_PASSWORD_CONFIG, properties[Key("KAFKA_CREDSTORE_PASSWORD", stringType)])
            put(SslConfigs.SSL_KEYSTORE_LOCATION_CONFIG, properties[Key("KAFKA_KEYSTORE_PATH", stringType)])
            put(SslConfigs.SSL_KEYSTORE_PASSWORD_CONFIG, properties[Key("KAFKA_CREDSTORE_PASSWORD", stringType)])
        }
    }

    object AzureAd {
        const val NAME = "azureAd"
        val audience = properties[Key("AZURE_APP_CLIENT_ID", stringType)]
        val issuer = properties[Key("AZURE_OPENID_CONFIG_ISSUER", stringType)]
        val jwksURI = properties[Key("AZURE_OPENID_CONFIG_JWKS_URI", stringType)]
    }

    fun Configuration.skjermingApiUrl() = this[Key("SKJERMING_API_URL", stringType)]

    fun Configuration.dpProxyUrl() = this[Key("DP_PROXY_HOST", stringType)].addHttprotocoll()

    fun Configuration.dpRegelApiUrl() = this[Key("DP_REGEL_API_URL", stringType)]

    fun Configuration.pdlApiUrl() = this[Key("PDL_API_HOST", stringType)].addHttprotocoll()

    fun asMap(): Map<String, String> =
        properties.list().reversed().fold(emptyMap()) { map, pair ->
            map + pair.second
        }
}

private fun getHostname(): String {
    return try {
        val addr: InetAddress = InetAddress.getLocalHost()
        addr.hostName
    } catch (e: UnknownHostException) {
        "unknown"
    }
}
