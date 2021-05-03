package no.nav.dagpenger.mottak

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class ByClusterStrategyTest {

    @Test
    fun `ByClusterStrategy skal tolke respons fra Unleash riktig`() {
        val byClusterStrategy = ByClusterStrategy(ByClusterStrategy.Cluster.DEV_GCP)
        mapOf(Pair("cluster", "dev-gcp,prod-gcp")).also {
            assertTrue(byClusterStrategy.isEnabled(it))
        }
        mapOf(Pair("cluster", "prod-gcp,dev-fss")).also {
            assertFalse(byClusterStrategy.isEnabled(it))
        }
    }
}
