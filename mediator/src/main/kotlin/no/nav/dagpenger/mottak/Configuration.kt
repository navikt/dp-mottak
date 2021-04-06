package no.nav.dagpenger.mottak

import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.overriding

internal object Configuration {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "KAFKA_CONSUMER_GROUP_ID" to "dp-mottak-v1",
            "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
            "KAFKA_RESET_POLICY" to "latest",
            "KAFKA_EXTRA_TOPIC" to "teamdagpenger.mottak.v1",
            "HTTP_PORT" to "8080"
        )
    )

    val properties by lazy {
        ConfigurationProperties.systemProperties() overriding EnvironmentVariables() overriding defaultProperties
    }

    fun asMap(): Map<String, String> = properties.list().reversed().fold(emptyMap()) { map, pair ->
        map + pair.second
    }
}
