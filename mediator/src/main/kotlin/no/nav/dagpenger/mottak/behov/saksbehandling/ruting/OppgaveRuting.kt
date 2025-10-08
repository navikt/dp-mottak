package no.nav.dagpenger.mottak.behov.saksbehandling.ruting

import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingKlient
import no.nav.dagpenger.mottak.behov.saksbehandling.SisteSakIdResult
import java.util.UUID

internal interface OppgaveRuting {
    suspend fun ruteOppgave(ident: String): System

    suspend fun ruteOppgave(
        ident: String,
        søknadsId: UUID,
    ): System

    enum class Fagsystem {
        DAGPENGER,
        ARENA,
    }

    sealed class System(val fagsystem: Fagsystem) {
        data class Dagpenger(val sakId: UUID) : System(Fagsystem.DAGPENGER)

        object Arena : System(Fagsystem.ARENA)
    }
}

internal class SakseierBasertRuting(private val saksbehandlingKlient: SaksbehandlingKlient) : OppgaveRuting {
    override suspend fun ruteOppgave(ident: String): OppgaveRuting.System {
        return saksbehandlingKlient.hentSisteSakId(ident).let {
            when (it) {
                SisteSakIdResult.IkkeFunnet -> OppgaveRuting.System.Arena
                is SisteSakIdResult.Funnet -> OppgaveRuting.System.Dagpenger(it.id)
            }
        }
    }

    override suspend fun ruteOppgave(
        ident: String,
        søknadsId: UUID,
    ): OppgaveRuting.System {
        TODO("Not yet implemented")
    }
}
