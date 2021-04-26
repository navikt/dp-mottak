package no.nav.dagpenger.mottak.db

import com.zaxxer.hikari.HikariDataSource
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.containers.PostgreSQLContainer.POSTGRESQL_PORT

internal object PostgresTestHelper {

    val instance by lazy {
        PostgreSQLContainer<Nothing>("postgres:12").apply {
            start()
        }
    }

    val dataSource by lazy {
        HikariDataSource().apply {
            dataSourceClassName = "org.postgresql.ds.PGSimpleDataSource"
            addDataSourceProperty("serverName", instance.host)
            addDataSourceProperty("portNumber", instance.getMappedPort(POSTGRESQL_PORT))
            addDataSourceProperty("databaseName", instance.databaseName)
            addDataSourceProperty("user", instance.username)
            addDataSourceProperty("password", instance.password)
            maximumPoolSize = 10
            minimumIdle = 1
            idleTimeout = 10001
            connectionTimeout = 1000
            maxLifetime = 30001
        }
    }

    fun withMigratedDb(block: () -> Unit) {
        withCleanDb {
            runMigration(dataSource)
            block()
        }
    }

    fun withCleanDb(block: () -> Unit) {
        clean(dataSource).run {
            block()
        }
    }
}
