package no.nav.dagpenger.mottak.behov.eksterne

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.db.PostgresDataSourceBuilder
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.postgresql.util.PGobject
import java.lang.IllegalArgumentException
import java.nio.charset.Charset

internal class PostgresSøknadQuizOppslagTest {
    private val journalpostID = 123L

    @Test
    fun hentSøknad() {
        withMigratedDb {
            val data = this.javaClass.getResource("/testdata/soknadsdata.json").readText(Charset.forName("UTF-8"))
            using(sessionOf(PostgresDataSourceBuilder.dataSource)) { session ->
                val id = session.run(
                    queryOf(
                        "INSERT INTO innsending_v1(journalpostId, tilstand) VALUES($journalpostID, 'superduper') RETURNING id",
                    ).map { row -> row.long("id") }.asSingle,
                )
                session.run(
                    queryOf(
                        "INSERT INTO soknad_v1(id,data) VALUES(:id, :data)",
                        mapOf(
                            "id" to id,
                            "data" to PGobject().apply {
                                type = "jsonb"
                                value = data
                            },
                        ),
                    ).asUpdate,
                )
            }

            PostgresSøknadQuizOppslag(PostgresDataSourceBuilder.dataSource).also {
                it.hentSøknad("0e7b27f5-5773-489b-a7f1-eaeb91da7761").also { søknadFakta ->
                    assertEquals("0e7b27f5-5773-489b-a7f1-eaeb91da7761", søknadFakta.søknadsId())
                }
                assertThrows<IllegalArgumentException> { it.hentSøknad("Nehehehei") }
            }
        }
    }
}
