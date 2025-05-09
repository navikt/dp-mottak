package no.nav.dagpenger.mottak.behov.saksbehandling.ruting

import no.nav.dagpenger.mottak.Config

internal interface OppgaveRuting {
    fun ruteOppgave(): FagSystem

    enum class FagSystem {
        DAGPENGER,
        ARENA,
    }
}

internal class MiljøBasertRuting() : OppgaveRuting {
    override fun ruteOppgave(): OppgaveRuting.FagSystem {
        return when (Config.env) {
            "dev-gcp" -> {
                OppgaveRuting.FagSystem.DAGPENGER
            }

            else -> {
                OppgaveRuting.FagSystem.ARENA
            }
        }
    }
}
