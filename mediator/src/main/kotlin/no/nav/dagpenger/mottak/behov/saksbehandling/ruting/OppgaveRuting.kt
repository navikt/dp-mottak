package no.nav.dagpenger.mottak.behov.saksbehandling.ruting

import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingKlient
import no.nav.dagpenger.mottak.behov.saksbehandling.SisteSakIdResult
import java.util.UUID

internal interface OppgaveRuting {
    suspend fun ruteOppgave(ident: String): System

    enum class FagSystem {
        DAGPENGER,
        ARENA,
    }

    sealed class System(val fagSystem: FagSystem) {
        data class Dagpenger(val sakId: UUID) : System(FagSystem.DAGPENGER)

        object Arena : System(FagSystem.ARENA)
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
}
