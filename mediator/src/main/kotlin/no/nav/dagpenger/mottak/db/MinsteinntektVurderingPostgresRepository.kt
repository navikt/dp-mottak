package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.mottak.behov.vilkårtester.MinsteinntektVurderingRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import org.postgresql.util.PGobject
import javax.sql.DataSource

internal class MinsteinntektVurderingPostgresRepository(private val dataSource: DataSource) :
    MinsteinntektVurderingRepository {

    private companion object {
        val logger = KotlinLogging.logger {}
    }
    override fun lagre(journalpostId: String, packet: JsonMessage): Int {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "INSERT INTO  minsteinntekt_vurdering_v1(journalpostId,packet) VALUES(:journalpostId,:packet) ON CONFLICT DO NOTHING",
                    mapOf(
                        "journalpostId" to journalpostId.toLong(),
                        "packet" to PGobject().apply {
                            type = "jsonb"
                            value = packet.toJson()
                        }
                    )
                ).asUpdate
            )
        }
    }

    override fun fjern(journalpostId: String): JsonMessage? {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf(
                    "DELETE FROM minsteinntekt_vurdering_v1 WHERE journalpostId=:journalpostId RETURNING *",
                    mapOf(
                        "journalpostId" to journalpostId.toLong()
                    )
                ).map { res ->
                    res.string("packet")
                }.asSingle
            )
        }?.let {
            JsonMessage(it, MessageProblems(it))
        }
    }

    override fun slettUtgåtteVurderinger(): List<Pair<String, JsonMessage>> {
        return if (lås()) {
            try {
                logger.info { "Fikk lås, kjører vaktmesterspørring" }
                return using(sessionOf(dataSource)) { session ->

                    session.run(
                        queryOf( //language=PostgreSQL
                            "DELETE FROM minsteinntekt_vurdering_v1 WHERE opprettet < (now() - (make_interval(mins := 5))) RETURNING *"
                        ).map { res ->
                            val jpId = res.string("journalpostId")
                            val packet = res.string("packet")
                            Pair(jpId, JsonMessage(packet, MessageProblems(packet)))
                        }.asList
                    )
                }
            } finally {
                logger.info { "Lås opp? ${låsOpp()}, ferdig med vaktmesterspørring" }
            }
        } else {
            logger.info { "Fikk IKKE lås, kjører IKKE vaktmesterspørring" }
            emptyList()
        }

    }



    private fun lås(): Boolean {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf( //language=PostgreSQL
                    "SELECT pg_advisory_lock(:key)", mapOf("key" to 573463)
                ).map { res ->
                    res.boolean(0)
                }.asSingle) ?: false
        }
    }
    private fun låsOpp(): Boolean {
        return using(sessionOf(dataSource)) { session ->
            session.run(
                queryOf( //language=PostgreSQL
                    "SELECT pg_advisory_unlock(:key)", mapOf("key" to 573463)
                ).map { res ->
                    res.boolean(1)
                }.asSingle) ?: false
        }
    }
}
