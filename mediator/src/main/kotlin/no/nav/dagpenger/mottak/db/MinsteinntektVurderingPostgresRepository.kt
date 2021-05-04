package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.behov.vilkårtester.MinsteinntektVurderingRepository
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import org.postgresql.util.PGobject
import javax.sql.DataSource

internal class MinsteinntektVurderingPostgresRepository(private val dataSource: DataSource) :
    MinsteinntektVurderingRepository {
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
        return using(sessionOf(dataSource)) { session ->
            //language=PostgreSQL
            session.run(
                queryOf(
                    "DELETE FROM minsteinntekt_vurdering_v1 WHERE opprettet < (now() - (make_interval(mins := 5))) RETURNING *"
                ).map { res ->
                    val jpId = res.string("journalpostId")
                    val packet = res.string("packet")
                    Pair(jpId, JsonMessage(packet, MessageProblems(packet)))
                }.asList
            )
        }
    }
}
