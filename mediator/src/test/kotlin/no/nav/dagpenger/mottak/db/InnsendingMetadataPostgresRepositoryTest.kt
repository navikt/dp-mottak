package no.nav.dagpenger.mottak.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.fnr
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import no.nav.dagpenger.mottak.serder.InnsendingData
import org.junit.jupiter.api.Test
import java.util.UUID
import javax.sql.DataSource

class InnsendingMetadataPostgresRepositoryTest {
    private val søknadId = UUID.randomUUID()

    @Test
    fun `skal kunne hente ut oppgaveIder og fagsakId for en gitt søknadId og ident`() {
        val søknad =
            innsendingData.copy(
                id = 1,
                søknadsData = søknadsData,
                arenaSakData =
                    InnsendingData.ArenaSakData(
                        oppgaveId = "søknad",
                        fagsakId = "fagsakid",
                    ),
                journalpostId = "1",
                journalpostData = innsendingData.journalpostData?.copy(journalpostId = "1"),
            )

        val ettersending =
            innsendingData.copy(
                id = 2,
                søknadsData = søknadsData,
                arenaSakData =
                    InnsendingData.ArenaSakData(
                        oppgaveId = "ettersending",
                        fagsakId = null,
                    ),
                journalpostId = "2",
                journalpostData = innsendingData.journalpostData?.copy(journalpostId = "1"),
            )
        withMigratedDb {
            val innsendingPostgresRepository = InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)
            innsendingPostgresRepository.apply {
                lagre(søknad.createInnsending())
                lagre(ettersending.createInnsending())
            }

            InnsendingMetadataPostgresRepository(PostgresDataSourceBuilder.dataSource).apply {
                val arenaSaker = hentArenaOppgaver(søknadId, fnr)
                arenaSaker shouldBe
                    listOf(
                        ArenaOppgave(
                            journalpostId = "1",
                            oppgaveId = "søknad",
                            fagsakId = "fagsakid",
                            innsendingId = 1,
                        ),
                        ArenaOppgave(
                            journalpostId = "2",
                            oppgaveId = "ettersending",
                            fagsakId = null,
                            innsendingId = 2,
                        ),
                    )
            }
        }
    }

    @Test
    fun `opprett ny JournalpostSak`() {
        withMigratedDb {
            val innsendingPostgresRepository = InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)
            innsendingPostgresRepository.lagre(innsendingData.createInnsending())
            val journalpostId = 112312
            val innsendingId = 1
            val fagsakId = UUID.randomUUID()
            InnsendingMetadataPostgresRepository(PostgresDataSourceBuilder.dataSource).apply {
                opprettKoblingTilNyJournalpostForSak(
                    jounalpostId = journalpostId,
                    innsendingId = innsendingId,
                    fagsakId = fagsakId,
                )
            }
            PostgresDataSourceBuilder.dataSource.sjekkAtNyJournalPostErLagret(journalpostId, fagsakId)
        }
    }

    @Test
    fun `hent journalposter for en søknad som er journalført på Dagpenger sak`() {
        val dpSakId = UUID.randomUUID()
        val søknad =
            innsendingData.copy(
                id = 1,
                søknadsData = søknadsData,
                arenaSakData = null,
                oppgaveSakData =
                    InnsendingData.OppgaveSakData(
                        oppgaveId = null,
                        fagsakId = dpSakId,
                    ),
                journalpostId = "1",
                journalpostData = innsendingData.journalpostData?.copy(journalpostId = "1"),
            )
        val ettersending =
            innsendingData.copy(
                id = 2,
                søknadsData = søknadsData,
                arenaSakData = null,
                oppgaveSakData =
                    InnsendingData.OppgaveSakData(
                        oppgaveId = null,
                        fagsakId = dpSakId,
                    ),
                journalpostId = "2",
                journalpostData = innsendingData.journalpostData?.copy(journalpostId = "2"),
            )
        withMigratedDb {
            val innsendingPostgresRepository = InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)
            innsendingPostgresRepository.apply {
                lagre(søknad.createInnsending())
                lagre(ettersending.createInnsending())
            }

            InnsendingMetadataPostgresRepository(PostgresDataSourceBuilder.dataSource).apply {
                hentJournalpostIder(søknadId, fnr) shouldBe listOf("1", "2")
            }
        }
    }

    @Test
    fun `hent journalposter for en søknad avhengig av om journalpost er knyttet til ny sak i Dagpenger eller kun til Arena sak`() {
        val søknad =
            innsendingData.copy(
                id = 1,
                søknadsData = søknadsData,
                arenaSakData =
                    InnsendingData.ArenaSakData(
                        oppgaveId = "søknad",
                        fagsakId = "fagsakid",
                    ),
                journalpostId = "1",
                journalpostData = innsendingData.journalpostData?.copy(journalpostId = "1"),
            )

        val ettersending =
            innsendingData.copy(
                id = 2,
                søknadsData = søknadsData,
                arenaSakData =
                    InnsendingData.ArenaSakData(
                        oppgaveId = "ettersending",
                        fagsakId = null,
                    ),
                journalpostId = "2",
                journalpostData = innsendingData.journalpostData?.copy(journalpostId = "2"),
            )

        withMigratedDb {
            val innsendingPostgresRepository = InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)
            innsendingPostgresRepository.apply {
                lagre(søknad.createInnsending())
                lagre(ettersending.createInnsending())
            }

            InnsendingMetadataPostgresRepository(PostgresDataSourceBuilder.dataSource).apply {
                val arenaJournalpostIder = hentJournalpostIder(søknadId, fnr)
                arenaJournalpostIder shouldBe listOf("1", "2")

                val nyJournalpostIdSøknad = 111
                val nyJournalpostIdEttersending = 222
                val nyDagpengerFagsakId = UUID.randomUUID()
                opprettKoblingTilNyJournalpostForSak(
                    jounalpostId = nyJournalpostIdSøknad,
                    innsendingId = søknad.id.toInt(),
                    fagsakId = nyDagpengerFagsakId,
                )
                opprettKoblingTilNyJournalpostForSak(
                    jounalpostId = nyJournalpostIdEttersending,
                    innsendingId = ettersending.id.toInt(),
                    fagsakId = nyDagpengerFagsakId,
                )

                PostgresDataSourceBuilder.dataSource.sjekkAtNyJournalPostErLagret(nyJournalpostIdSøknad, nyDagpengerFagsakId)
                PostgresDataSourceBuilder.dataSource.sjekkAtNyJournalPostErLagret(nyJournalpostIdEttersending, nyDagpengerFagsakId)
                val dagpengerJournalpostIder = hentJournalpostIder(søknadId, fnr)
                dagpengerJournalpostIder shouldBe listOf("111", "222")
            }
        }
    }

    fun DataSource.sjekkAtNyJournalPostErLagret(
        journalpostId: Int,
        fagsakId: UUID,
    ) {
        using(sessionOf(this)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """SELECT count(*) FROM journalpost_dagpenger_sak_v1 
                        WHERE journalpost_id = :journalpost_id AND fagsak_id = :fagsak_id 
                    """.trimMargin(),
                    mapOf(
                        "journalpost_id" to journalpostId,
                        "fagsak_id" to fagsakId,
                    ),
                ).map { row ->
                    row.int(1)
                }.asSingle,
            )
        } shouldBe 1
    }

    private val søknadsData =
        jacksonObjectMapper().readTree(
            """
            {
                "søknad_uuid": "$søknadId"
            }
            """.trimIndent(),
        )
}
