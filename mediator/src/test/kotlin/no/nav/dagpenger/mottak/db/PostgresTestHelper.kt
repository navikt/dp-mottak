package no.nav.dagpenger.mottak.db

import org.flywaydb.core.internal.configuration.ConfigUtils
import org.testcontainers.containers.wait.strategy.HostPortWaitStrategy
import org.testcontainers.postgresql.PostgreSQLContainer

internal object PostgresTestHelper {
    val instance by lazy {
        PostgreSQLContainer("postgres:18.1").apply {
            this.waitingFor(HostPortWaitStrategy())
            start()
        }
    }

    fun withMigratedDb(block: () -> Unit) {
        withCleanDb {
            PostgresDataSourceBuilder.runMigration()
            block()
        }
    }

    fun withCleanDb(block: () -> Unit) {
        setup()
        PostgresDataSourceBuilder
            .clean()
            .run {
                block()
            }.also {
                tearDown()
            }
    }

    fun setup() {
        System.setProperty(ConfigUtils.CLEAN_DISABLED, "false")
        System.setProperty(PostgresDataSourceBuilder.DB_URL_KEY, instance.jdbcUrl)
        System.setProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY, instance.password)
        System.setProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY, instance.username)
    }

    fun tearDown() {
        System.clearProperty(PostgresDataSourceBuilder.DB_PASSWORD_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_USERNAME_KEY)
        System.clearProperty(PostgresDataSourceBuilder.DB_URL_KEY)
        System.clearProperty(ConfigUtils.CLEAN_DISABLED)
    }
}
