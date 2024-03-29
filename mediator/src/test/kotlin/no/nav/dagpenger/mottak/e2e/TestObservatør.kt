package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType

internal class TestObservatør : InnsendingObserver {
    internal val tilstander = mutableListOf<InnsendingTilstandType>()

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        tilstander.add(event.gjeldendeTilstand)
    }

    fun reset() {
        tilstander.clear()
    }
}
