package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.db.PostgresTestHelper.withCleanDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SkjemaTest {
    @Test
    fun `riktig antall migreringer`() {
        withCleanDb {
            assertEquals(20, PostgresDataSourceBuilder.runMigration())
        }
    }
}
