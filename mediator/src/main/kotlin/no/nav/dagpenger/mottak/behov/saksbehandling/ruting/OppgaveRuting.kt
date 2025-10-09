package no.nav.dagpenger.mottak.behov.saksbehandling.ruting

import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingKlient
import no.nav.dagpenger.mottak.behov.saksbehandling.SisteSakIdResult
import java.util.UUID

internal interface OppgaveRuting {
    suspend fun ruteOppgave(ident: String): Fagsystem

    suspend fun ruteOppgave(
        ident: String,
        søknadsId: UUID,
    ): Fagsystem
}

internal class SakseierBasertRuting(private val saksbehandlingKlient: SaksbehandlingKlient) : OppgaveRuting {
    override suspend fun ruteOppgave(ident: String): Fagsystem {
        return saksbehandlingKlient.hentSisteSakId(ident).let {
            when (it) {
                SisteSakIdResult.IkkeFunnet -> Fagsystem.Arena
                is SisteSakIdResult.Funnet -> Fagsystem.Dagpenger(it.id)
            }
        }
    }

    override suspend fun ruteOppgave(
        ident: String,
        søknadsId: UUID,
    ): Fagsystem {
        // returner kun dagpenger hvis  søknad har vedtak i dagpenger system.

        TODO("Not yet implemented")
    }
}
