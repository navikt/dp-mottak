package no.nav.dagpenger.mottak

import com.natpryce.konfig.Configuration
import com.natpryce.konfig.ConfigurationMap
import com.natpryce.konfig.ConfigurationProperties
import com.natpryce.konfig.EnvironmentVariables
import com.natpryce.konfig.Key
import com.natpryce.konfig.intType
import com.natpryce.konfig.overriding
import com.natpryce.konfig.stringType
import com.zaxxer.hikari.HikariDataSource
import io.netty.util.NetUtil.getHostname
import no.finn.unleash.DefaultUnleash
import no.finn.unleash.util.UnleashConfig
import no.nav.dagpenger.aad.api.ClientCredentialsClient
import java.net.InetAddress
import java.net.UnknownHostException

internal object Config {

    private val defaultProperties = ConfigurationMap(
        mapOf(
            "DB_USERNAME" to "username",
            "DB_PASSWORD" to "password",
            "DB_DATABASE" to "dp-mottak",
            "DB_HOST" to "localhost",
            "DB_PORT" to "5432",
            "DP_PROXY_SCOPE" to "api://dev-fss.teamdagpenger.dp-proxy/.default",
            "DP_PROXY_URL" to "https://dp-proxy.dev-fss-pub.nais.io",
            "HTTP_PORT" to "8080",
            "KAFKA_CONSUMER_GROUP_ID" to "dp-mottak-v1",
            "KAFKA_EXTRA_TOPIC" to "teamdagpenger.mottak.v1,teamdagpenger.regel.v1",
            "KAFKA_RAPID_TOPIC" to "teamdagpenger.rapid.v1",
            "KAFKA_RESET_POLICY" to "latest",
            "UNLEASH_URL" to "https://unleash.nais.io/api/"

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

    val Configuration.tokenProvider by lazy {
        ClientCredentialsClient(properties) {
            scope {
                add(properties.dpProxyScope())
            }
        }
    }

    val dataSource by lazy {
        HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", properties[Key("DB_HOST", stringType)])
            addDataSourceProperty("portNumber", properties[Key("DB_PORT", intType)])
            addDataSourceProperty("databaseName", properties[Key("DB_DATABASE", stringType)])
            addDataSourceProperty("user", properties[Key("DB_USERNAME", stringType)])
            addDataSourceProperty("password", properties[Key("DB_PASSWORD", stringType)])
            maximumPoolSize = 10
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
    }

    fun Configuration.dpProxyUrl() = this[Key("DP_PROXY_URL", stringType)]
    fun Configuration.dpProxyScope() = this[Key("DP_PROXY_SCOPE", stringType)]
    fun Configuration.unleash() = DefaultUnleash(unleashConfig(), ByClusterStrategy(ByClusterStrategy.Cluster.current))
    private fun Configuration.unleashConfig(): UnleashConfig =
        UnleashConfig.builder()
            .appName("dp-mottak")
            .instanceId(getHostname())
            .unleashAPI(this[Key("unleash.url", stringType)])
            .build()
    fun asMap(): Map<String, String> = properties.list().reversed().fold(emptyMap()) { map, pair ->
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
