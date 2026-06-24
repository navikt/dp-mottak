package no.nav.dagpenger.mottak.db

import ch.qos.logback.core.util.OptionHelper
import com.zaxxer.hikari.HikariDataSource
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.configuration.ConfigUtils
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

internal object PostgresDataSourceBuilder {
    const val DB_USERNAME_KEY = "DB_USERNAME"
    const val DB_PASSWORD_KEY = "DB_PASSWORD"
    const val DB_URL_KEY = "DB_URL"

    private fun getOrThrow(key: String): String = OptionHelper.getEnv(key) ?: OptionHelper.getSystemProperty(key) ?: throw IllegalArgumentException("Missing required environment variable $key")

    val dataSource =
        HikariDataSource().apply {
            jdbcUrl = getOrThrow(DB_URL_KEY).ensurePrefix("jdbc:postgresql://").stripCredentials()
            username = getOrThrow(DB_USERNAME_KEY)
            password = getOrThrow(DB_PASSWORD_KEY)

            // Default 30 sekund
            connectionTimeout = 10.seconds.inWholeMilliseconds
            // Default 10 minutter
            idleTimeout = 10.minutes.inWholeMilliseconds
            // Default 2 minutter
            keepaliveTime = 2.minutes.inWholeMilliseconds
            // Default 30 minutter
            maxLifetime = 30.minutes.inWholeMilliseconds
            leakDetectionThreshold = 10.seconds.inWholeMilliseconds
            metricRegistry =
                PrometheusMeterRegistry(
                    PrometheusConfig.DEFAULT,
                    PrometheusRegistry.defaultRegistry,
                    Clock.SYSTEM,
                )
        }

    internal fun clean() =
        Flyway
            .configure()
            .cleanDisabled(getOrThrow(ConfigUtils.CLEAN_DISABLED).toBooleanStrict())
            .dataSource(dataSource)
            .load()
            .clean()

    internal fun runMigration(initSql: String? = null): Int =
        Flyway
            .configure()
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
            .migrations.size
}

private fun String.ensurePrefix(prefix: String) =
    if (this.startsWith(prefix)) {
        this
    } else {
        prefix + this.substringAfter("//")
    }

private fun String.stripCredentials() = this.replace(Regex("://.*:.*@"), "://")
