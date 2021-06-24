package no.nav.dagpenger.mottak.db

import org.flywaydb.core.Flyway
import javax.sql.DataSource

internal fun clean(ds: DataSource) = Flyway.configure().dataSource(ds).load().clean()

internal fun runMigration(ds: DataSource, initSql: String? = null): Int =
    Flyway.configure()
        .dataSource(ds)
        .initSql(initSql)
        .load()
        .migrate()
        .migrations.size
