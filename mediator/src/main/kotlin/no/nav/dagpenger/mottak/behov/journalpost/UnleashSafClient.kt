package no.nav.dagpenger.mottak.behov.journalpost

import com.natpryce.konfig.Configuration
import io.getunleash.Unleash

internal class UnleashSafClient(config: Configuration, private val unleash: Unleash) : JournalpostArkiv, SøknadsArkiv {
    private val proxy = SafProxyClient(config)
    private val safClient = SafClient(config)

    companion object {
        const val TOGGLE = "bruk-saf-client"
    }

    override suspend fun hentJournalpost(journalpostId: String): SafGraphQL.Journalpost {
        return if (unleash.isEnabled(TOGGLE)) {
            safClient.hentJournalpost(journalpostId)
        } else {
            proxy.hentJournalpost(journalpostId)
        }
    }

    override suspend fun hentSøknadsData(
        journalpostId: String,
        dokumentInfoId: String,
    ): SafGraphQL.SøknadsData {
        return if (unleash.isEnabled(TOGGLE)) {
            safClient.hentSøknadsData(journalpostId, dokumentInfoId)
        } else {
            proxy.hentSøknadsData(journalpostId, dokumentInfoId)
        }
    }
}
