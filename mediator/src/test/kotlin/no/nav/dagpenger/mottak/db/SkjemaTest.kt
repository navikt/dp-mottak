package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.db.PostgresTestHelper.dataSource
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withCleanDb
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

internal class SkjemaTest {

    @Test
    fun `riktig anntall migreringer`() {
        withCleanDb {
            assertEquals(4, runMigration(dataSource))
        }
    }
}
