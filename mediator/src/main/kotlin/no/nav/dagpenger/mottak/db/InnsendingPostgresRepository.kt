package no.nav.dagpenger.mottak.db

import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingVisitor
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

    override fun lagre(innsending: Innsending): Boolean {
        val visitor = PersistenceVisitor(innsending)
        return using(sessionOf(datasource)) { session ->
            val updates = visitor.lagreQueries.map { query ->
                session.transaction { tx ->
                    tx.run(query.asUpdate)
                }
            }
            updates.all { it==1 }
        }
    }

    private class PersistenceVisitor(val innsending: Innsending) : InnsendingVisitor {

        val lagreQueries: MutableList<Query> = mutableListOf()

        init {
            innsending.accept(this)
        }

        override fun visitTilstand(tilstandType: Innsending.Tilstand) {
            lagreQueries.add(queryOf(//language=PostgreSQL
                "INSERT INTO innsending_v1(journalpostId,tilstand) VALUES(:jpId, :tilstand)", mapOf(
                    "jpId" to innsending.journalpostId().toLong(),
                    "tilstand" to tilstandType.type.name
                )
            ))
        }
    }
}
