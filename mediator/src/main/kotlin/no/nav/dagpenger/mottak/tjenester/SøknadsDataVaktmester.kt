package no.nav.dagpenger.mottak.tjenester

import kotlinx.coroutines.runBlocking
import kotliquery.Session
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import mu.KotlinLogging
import no.nav.dagpenger.mottak.behov.journalpost.SafClient
import no.nav.dagpenger.mottak.db.PostgresDataSourceBuilder
import org.postgresql.util.PGobject
import javax.sql.DataSource

private val logger = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadsDataVaktmester")

internal class SøknadsDataVaktmester(
    private val safClient: SafClient,
    private val ds: DataSource = PostgresDataSourceBuilder.dataSource
) {

    fun fixSoknadsData(jp: Int) {
        using(sessionOf(ds)) { session ->
            try {
                if (session.lås(jp)) {
                    val (innsendingId, dokumentinfoId) = session.innsendingIdOgDokumentinfoId(jp)
                    runBlocking {
                        val data = safClient.hentSøknadsData(jp.toString(), dokumentinfoId.toString()).data.toString()
                        session.insertSøknadsData(innsendingId, data)
                        logger.info { "Oppdatert søknads data for journalpost id $jp" }
                        sikkerlogg.info { "Oppdatert søknads data for journalpost id $jp. Søknadsdata: $data" }
                    }
                }
            } catch (e: Exception) {
                logger.error(e) { "Feilet rydde jobb for jp: $jp" }
            } finally {
                session.låsOpp(jp)
            }
        }
    }

    private fun Session.insertSøknadsData(id: Int, data: String) {
        this.run(
            queryOf(
                //language=PostgreSQL
                statement = """INSERT INTO soknad_v1(id,data) VALUES(:id,:data) ON CONFLICT(id) DO UPDATE SET data = :data """,
                paramMap = mapOf(
                    "id" to id,
                    "data" to PGobject().apply {
                        type = "jsonb"
                        value = data
                    }
                )
            ).asUpdate
        )
    }

    private fun Session.innsendingIdOgDokumentinfoId(jp: Int): Pair<Int, Int> {
        return this.run(
            queryOf(
                //language=PostgreSQL
                statement = """
                            select innsending_v1.id,v.dokumentinfoid
                            from innsending_v1
                            join journalpost_v1 j on innsending_v1.id = j.id
                            join journalpost_dokumenter_v1 v on j.id = v.id
                            where  journalpostid = :journalpostId
                            and  v.hoveddokument = true """,
                paramMap = mapOf("journalpostId" to jp)
            ).map { row ->
                Pair(
                    first = row.int("id"),
                    second = row.int("dokumentinfoid"),
                )
            }.asSingle
        ) ?: throw RuntimeException("Fant ikke innsendingD/dokumentinfoid for $jp")
    }

    private fun Session.låsOpp(låseNøkkel: Int): Boolean {
        return this.run(
            queryOf( //language=PostgreSQL
                "SELECT pg_advisory_unlock(:key)",
                mapOf("key" to låseNøkkel)
            ).map { res ->
                res.boolean("pg_advisory_unlock")
            }.asSingle
        ) ?: false
    }

    private fun Session.lås(låseNøkkel: Int): Boolean {
        return this.run(
            queryOf( //language=PostgreSQL
                "SELECT pg_try_advisory_lock(:key)",
                mapOf("key" to låseNøkkel)
            ).map { res ->
                res.boolean("pg_try_advisory_lock")
            }.asSingle
        ) ?: false
    }
}
