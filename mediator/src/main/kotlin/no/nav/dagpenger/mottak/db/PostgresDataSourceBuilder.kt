package no.nav.dagpenger.mottak.db

import ch.qos.logback.core.util.OptionHelper
import com.zaxxer.hikari.HikariDataSource
import org.flywaydb.core.Flyway
import org.flywaydb.core.internal.configuration.ConfigUtils

internal object PostgresDataSourceBuilder {
    const val DB_USERNAME_KEY = "DB_USERNAME"
    const val DB_PASSWORD_KEY = "DB_PASSWORD"
    const val DB_DATABASE_KEY = "DB_DATABASE"
    const val DB_HOST_KEY = "DB_HOST"
    const val DB_PORT_KEY = "DB_PORT"

    private fun getOrThrow(key: String): String = OptionHelper.getEnv(key) ?: OptionHelper.getSystemProperty(key)

    val dataSource by lazy {
        HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", getOrThrow(DB_HOST_KEY))
            addDataSourceProperty("portNumber", getOrThrow(DB_PORT_KEY))
            addDataSourceProperty("databaseName", getOrThrow(DB_DATABASE_KEY))
            addDataSourceProperty("user", getOrThrow(DB_USERNAME_KEY))
            addDataSourceProperty("password", getOrThrow(DB_PASSWORD_KEY))
            maximumPoolSize = 10
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
    }

    internal fun clean() =
        Flyway.configure()
            .cleanDisabled(getOrThrow(ConfigUtils.CLEAN_DISABLED).toBooleanStrict())
            .dataSource(dataSource).load().clean()

    internal fun runMigration(initSql: String? = null): Int =
        Flyway.configure()
            .dataSource(dataSource)
            .initSql(initSql)
            .load()
            .migrate()
            .migrations.size
}
