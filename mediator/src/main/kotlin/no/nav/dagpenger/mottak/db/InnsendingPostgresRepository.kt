package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingTilstandType
import javax.sql.DataSource

internal class InnsendingPostgresRepository(private val datasource: DataSource = Config.dataSource) :
    InnsendingRepository {
    override fun hent(journalpostId: String): Innsending =
        using(sessionOf(datasource)) { session ->
            session.run(
                queryOf( //language=PostgreSQL
                    "SELECT * FROM innsending_v1 WHERE journalpostId = :id",
                    mapOf("id" to journalpostId.toLong())
                ).map { Innsending(it.long("journalpostId").toString()) }.asSingle
            )?.let {
                it
            }
        } ?: throw IllegalArgumentException("Kunne ikke finnne innsending med id $journalpostId")

    override fun lagre(innsending: Innsending): Boolean =
        using(sessionOf(datasource)) { session ->
            session.run(
                queryOf( //language=PostgreSQL
                    "INSERT INTO  innsending_v1(journalpostId, tilstand) VALUES (:journalpostId,:tilstand)",
                    dummyInnsendingVerider(innsending)
                ).asUpdate
            ).let { it == 1 }
        }
}

private fun dummyInnsendingVerider(innsending: Innsending) = mapOf(
    "journalpostId" to innsending.journalpostId().toLong(),
    "tilstand" to InnsendingTilstandType.MottattType.name
)
