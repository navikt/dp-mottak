package no.nav.dagpenger.mottak

import com.natpryce.konfig.Key
import com.natpryce.konfig.stringType
import io.getunleash.DefaultUnleash
import io.getunleash.FakeUnleash
import io.getunleash.Unleash
import io.getunleash.util.UnleashConfig
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Config.properties
import java.net.InetAddress

private val logger = KotlinLogging.logger { }

val unleash: Unleash by lazy {
    if (properties.getOrNull(Key("UNLEASH_SERVER_API_URL", stringType)) == null) return@lazy FakeUnleash()
    DefaultUnleash(
        UnleashConfig.builder()
            .appName(properties[Key("NAIS_APP_NAME", stringType)])
            .instanceId(runCatching { InetAddress.getLocalHost().hostName }.getOrElse { "ukjent" })
            .unleashAPI(properties[Key("UNLEASH_SERVER_API_URL", stringType)] + "/api/")
            .apiKey(properties[Key("UNLEASH_SERVER_API_TOKEN", stringType)])
            .environment(
                when (System.getenv("NAIS_CLUSTER_NAME").orEmpty()) {
                    "prod-gcp" -> "production"
                    else -> "development"
                },
            ).build().also {
                logger.info { "Unleash er konfigurert" }
            },
    )
}
