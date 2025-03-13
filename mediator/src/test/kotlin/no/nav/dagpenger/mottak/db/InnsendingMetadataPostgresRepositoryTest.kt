package no.nav.dagpenger.mottak.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.ettersendingDokumenter
import no.nav.dagpenger.fnr
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import no.nav.dagpenger.mottak.serder.InnsendingData
import no.nav.dagpenger.registrertdato
import no.nav.dagpenger.søknadDokumenter
import org.junit.jupiter.api.Test
import java.util.UUID

class InnsendingMetadataPostgresRepositoryTest {
    private val søknadId = UUID.randomUUID()

    @Test
    fun `skal kunne hente ut oppgaveIder for en gitt søknadId og ident`() {
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
                journalpostData =
                    InnsendingData.JournalpostData(
                        journalpostId = "1",
                        journalpostStatus = "aktiv",
                        bruker = InnsendingData.JournalpostData.BrukerData(InnsendingData.JournalpostData.BrukerTypeData.FNR, fnr),
                        behandlingstema = "DAG",
                        registertDato = registrertdato,
                        journalførendeEnhet = "ENHET",
                        dokumenter = søknadDokumenter,
                    ),
            )

        val ettersending =
            søknad.copy(
                id = 2,
                arenaSakData =
                    InnsendingData.ArenaSakData(
                        oppgaveId = "ettersending",
                        fagsakId = "fagsakid",
                    ),
                journalpostId = "2",
                journalpostData =
                    InnsendingData.JournalpostData(
                        journalpostId = "2",
                        journalpostStatus = "aktiv",
                        bruker = InnsendingData.JournalpostData.BrukerData(InnsendingData.JournalpostData.BrukerTypeData.FNR, fnr),
                        behandlingstema = "DAG",
                        registertDato = registrertdato,
                        journalførendeEnhet = "ENHET",
                        dokumenter = ettersendingDokumenter,
                    ),
            )
        withMigratedDb {
            val innsendingPostgresRepository = InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)
            innsendingPostgresRepository.apply {
                lagre(søknad.createInnsending())
                lagre(ettersending.createInnsending())
            }

            InnsendingMetadataPostgresRepository(PostgresDataSourceBuilder.dataSource).apply {
                val oppgaverIder = hentOppgaverIder(søknadId, fnr)
                oppgaverIder shouldBe listOf("søknad", "ettersending")
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
