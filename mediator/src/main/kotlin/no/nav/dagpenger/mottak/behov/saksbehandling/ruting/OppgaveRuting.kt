package no.nav.dagpenger.mottak.behov.saksbehandling.ruting

import no.nav.dagpenger.mottak.System
import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingKlient
import no.nav.dagpenger.mottak.behov.saksbehandling.SisteSakIdResult
import java.util.UUID

internal interface OppgaveRuting {
    suspend fun ruteOppgave(ident: String): System

    suspend fun ruteOppgave(
        ident: String,
        søknadsId: UUID,
    ): System
}

internal class SakseierBasertRuting(private val saksbehandlingKlient: SaksbehandlingKlient) : OppgaveRuting {
    override suspend fun ruteOppgave(ident: String): System {
        return saksbehandlingKlient.hentSisteSakId(ident).let {
            when (it) {
                SisteSakIdResult.IkkeFunnet -> System.Arena
                is SisteSakIdResult.Funnet -> System.Dagpenger(it.id)
            }
        }
    }

    override suspend fun ruteOppgave(
        ident: String,
        søknadsId: UUID,
    ): System {
        // returner kun dagpenger hvis  søknad har vedtak i dagpenger system.

        TODO("Not yet implemented")
    }
}
