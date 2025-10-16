package no.nav.dagpenger.mottak.behov.saksbehandling.ruting

import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.behov.saksbehandling.SakIdResponse
import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingKlient
import java.util.UUID

internal interface OppgaveRuting {
    suspend fun ruteOppgave(ident: String): Fagsystem

    suspend fun ruteOppgave(søknadsId: UUID): Fagsystem
}

internal class SakseierBasertRuting(private val saksbehandlingKlient: SaksbehandlingKlient) : OppgaveRuting {
    override suspend fun ruteOppgave(ident: String): Fagsystem {
        return saksbehandlingKlient.hentSisteSakId(ident).let { sakIdResponse ->
            when (sakIdResponse) {
                SakIdResponse.IkkeFunnet -> Fagsystem.Arena
                is SakIdResponse.Funnet -> Fagsystem.Dagpenger(sakId = sakIdResponse.id)
            }
        }
    }

    override suspend fun ruteOppgave(
        søknadsId: UUID,
    ): Fagsystem {
        return saksbehandlingKlient.hentSakIdForSøknad(søknadsId).let { sakIdResponse ->
            when (sakIdResponse) {
                SakIdResponse.IkkeFunnet -> Fagsystem.Arena
                is SakIdResponse.Funnet -> Fagsystem.Dagpenger(sakId = sakIdResponse.id)
            }
        }
    }
}
