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

    companion object {
        fun SøknadsDataVaktmester.fixManglendeSøknadsData() {
            listOf(
                598734831,
                598734768,
                598734550,
                598734543,
                598734505,
                598733947,
                598733824,
                598733774,
                598733772,
                598733418,
                598733340,
                598733086,
                598733057,
                598733049,
                598732905,
                598732625,
                598732589,
                598732449,
                598732357,
                598732309,
                598732156,
                598732180,
                598732099,
                598732065,
                598732013,
                598731918,
                598731781,
                598731591,
                598731492,
                598731295,
                598731276,
                598731120,
                598731165,
                598731104,
                598730976,
                598730956,
                598730955,
                598730797,
                598730535,
                598730564,
                598730310,
                598730275,
                598730242,
                598730093,
                598730014,
                598729660,
                598729564,
                598729583,
                598729518,
                598729079,
                598728831,
                598728565,
                598727494,
                598727329,
                598727090,
                598726948,
                598726455,
                598726240,
                598725497,
                598725099,
                598725044,
                598724664,
                598723519,
                598723367,
                598722464,
                598722336,
                598722085,
                598721461,
                598721424,
                598721410,
                598721112,
                598720355,
                598719943,
                598719566,
                598717832,
                598716041,
                598715493,
                598715253,
                598714770,
                598714572,
                598714497,
                598714274,
                598713745,
                598713654,
                598713312,
                598713090,
                598712351,
                598712329,
                598712332,
                598712139,
                598711714,
                598711677,
                598710150,
                598709559,
                598709517,
                598709011,
                598708500,
                598708036,
                598708026,
                598707424,
                598707015,
                598706959,
                598706671,
                598705889,
                598703636,
                598703580,
                598703441,
                598701520,
                598699101,
                598698756,
                598698447,
                598697914,
                598697348,
                598697214,
                598696850,
                598696587,
                598696357,
                598696105,
                598695583,
                598695011,
                598694295,
                598694046,
                598693593,
                598692752,
                598692731,
                598692575,
                598692027,
                598691797,
                598691437,
                598691242,
                598690721,
                598690348,
                598689096,
                598687667,
                598687598,
                598687503,
                598687077,
                598686658,
                598685038
            ).forEach { jp ->
                this.fixSoknadsData(jp)
            }
        }
    }

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
