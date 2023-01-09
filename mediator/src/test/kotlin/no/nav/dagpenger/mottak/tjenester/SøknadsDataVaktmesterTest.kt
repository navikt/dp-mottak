package no.nav.dagpenger.mottak.tjenester

import io.mockk.coEvery
import io.mockk.mockk
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.mottak.behov.JsonMapper
import no.nav.dagpenger.mottak.behov.journalpost.SafClient
import no.nav.dagpenger.mottak.behov.journalpost.SafGraphQL
import no.nav.dagpenger.mottak.db.InnsendingPostgresRepository
import no.nav.dagpenger.mottak.db.PostgresDataSourceBuilder
import no.nav.dagpenger.mottak.db.PostgresTestHelper
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class SøknadsDataVaktmesterTest {

    @Test
    fun `Skal overskrive søknads data`() {
        val innsending = innsendingData.createInnsending()
        val jp = innsending.journalpostId().toInt()
        PostgresTestHelper.withMigratedDb {
            InnsendingPostgresRepository().lagre(innsending)

            SøknadsDataVaktmester(
                safClient = mockk<SafClient>().also {
                    coEvery {
                        it.hentSøknadsData(innsending.journalpostId(), "12345678")
                    } returns SafGraphQL.SøknadsData.fromJson("""{"hubba": "bubba"}""")
                },
            ).fixSoknadsData(jp)

            using(sessionOf(PostgresDataSourceBuilder.dataSource)) { session ->
                session.run(
                    queryOf(
                        statement = """SELECT data FROM soknad_v1 where id = :id""",
                        paramMap = mapOf("id" to innsendingData.id)
                    ).map { row -> row.binaryStream("data") }.asSingle
                ).let { JsonMapper.jacksonJsonAdapter.readTree(it) }.also {
                    assertEquals("bubba", it["hubba"].asText())
                }
            }
        }
    }
}
