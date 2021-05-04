package no.nav.dagpenger.mottak

import no.finn.unleash.strategy.Strategy

internal class ByClusterStrategy(private val currentCluster: Cluster) : Strategy {

    companion object {
        const val SLÅ_PÅ_HÅNDTERING = "dagpenger-journalforing-ferdigstill.disable"
    }
    override fun getName(): String = "byCluster"
    override fun isEnabled(parameters: Map<String, String>): Boolean {
        val clustersParameter = parameters["cluster"] ?: return false
        val alleClustere = clustersParameter.split(",").map { it.trim() }.map { it.toLowerCase() }.toList()
        return alleClustere.contains(currentCluster.asString())
    }

    enum class Cluster {
        DEV_GCP, PROD_GCP, ANNET;

        companion object {
            val current: Cluster by lazy {
                when (System.getenv("NAIS_CLUSTER_NAME")) {

                    "dev-gcp" -> DEV_GCP
                    "prod-gcp" -> PROD_GCP
                    else -> ANNET
                }
            }
        }

        fun asString(): String = name.toLowerCase().replace("_", "-")
    }
}
