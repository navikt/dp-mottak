package no.nav.dagpenger.mottak

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType

internal object Config {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "DP_PROXY_SCOPE" to "api://dev-fss.teamdagpenger.dp-proxy/.default",
            "DP_PROXY_URL" to "https://dp-proxy.dev-fss-pub.nais.io",
            "HTTP_PORT" to "8080",
            "KAFKA_CONSUMER_GROUP_ID" to "dp-mottak-v1",
            "KAFKA_EXTRA_TOPIC" to "teamdagpenger.mottak.v1",
            "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
            "KAFKA_RESET_POLICY" to "latest"
        )
    )
    private val prodProperties = ConfigurationMap(
        mapOf(
            "DP_PROXY_SCOPE" to "api://prod-fss.teamdagpenger.dp-proxy/.default",
            "DP_PROXY_URL" to "https://dp-proxy.prod-fss-pub.nais.io"
        )
    )

    val properties: Configuration by lazy {
        val systemAndEnvProperties = ConfigurationProperties.systemProperties() overriding EnvironmentVariables()
        when (System.getenv().getOrDefault("NAIS_CLUSTER_NAME", "LOCAL")) {
            "prod-gcp" -> systemAndEnvProperties overriding prodProperties overriding defaultProperties
            else -> systemAndEnvProperties overriding defaultProperties
        }
    }

    fun Configuration.dpProxyUrl() = this[Key("DP_PROXY_URL", stringType)]
    fun Configuration.dpProxyScope() = this[Key("DP_PROXY_SCOPE", stringType)]

    fun asMap(): Map<String, String> = properties.list().reversed().fold(emptyMap()) { map, pair ->
        map + pair.second
    }
}
