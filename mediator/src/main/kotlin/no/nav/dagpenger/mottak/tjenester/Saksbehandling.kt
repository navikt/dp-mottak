package no.nav.dagpenger.mottak.tjenester

import io.ktor.client.HttpClient

interface SaksbehandlingKlient {
    suspend fun finnesSøknadTilBehandling(
        søknadId: String,
        ident: String,
    ): Boolean
}

class Saksbehandling(private val httpClient: HttpClient) : SaksbehandlingKlient {
    override suspend fun finnesSøknadTilBehandling(
        søknadId: String,
        ident: String,
    ): Boolean {
        // TODO: Kall dp-saksbehandling for å sjekke om søknad er tatt til behandling der
        return true
    }
}
