package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType

class TestObservatør : InnsendingObserver {
    internal val tilstander = mutableMapOf<String, MutableList<InnsendingTilstandType>>()
    internal var event: InnsendingObserver.InnsendingEvent? = null
    internal var mottattEvent: InnsendingObserver.InnsendingEvent? = null

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        tilstander.getOrPut(event.journalpostId) { mutableListOf(InnsendingTilstandType.MottattType) }.add(event.gjeldendeTilstand)
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        this.event = event
    }

    override fun innsendingMottatt(event: InnsendingObserver.InnsendingEvent) {
        mottattEvent = event
    }
}
