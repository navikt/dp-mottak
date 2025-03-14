package no.nav.dagpenger.mottak.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.fnr
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.serder.InnsendingData
import org.junit.jupiter.api.Test
import java.util.UUID

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
                val arenaSaker = hentArenaSak(søknadId, fnr)
                arenaSaker shouldBe
                    listOf(
                        ArenaOppgaveOpprettet.ArenaSak(
                            oppgaveId = "søknad",
                            fagsakId = "fagsakid",
                        ),
                        ArenaOppgaveOpprettet.ArenaSak(
                            oppgaveId = "ettersending",
                            fagsakId = null,
                        ),
                    )
            }
        }
    }

    private val søknadsData =
        jacksonObjectMapper().readTree(
            """
            {
                "@id": "$søknadId"
            }
            """.trimIndent(),
        )
}
