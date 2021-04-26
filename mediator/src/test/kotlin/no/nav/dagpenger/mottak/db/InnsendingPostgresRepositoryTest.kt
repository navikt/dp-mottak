package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class InnsendingPostgresRepositoryTest {

    private val journalpostId = "187689"

    @Test
    fun `hent skal kunne hente innsending`() {
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresTestHelper.dataSource)) {
                lagre(Innsending((journalpostId))).also {
                    assertTrue(it)
                }

                hent(journalpostId).also {
                    assertEquals(journalpostId, it.journalpostId())
                }
            }
        }
    }
}
