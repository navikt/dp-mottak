package no.nav.dagpenger.mottak.observers

import io.prometheus.client.Counter
import no.nav.dagpenger.mottak.InnsendingObserver

internal class MetrikkObserver : InnsendingObserver {

    companion object {
        private val tilstandCounter = Counter
            .build("dp_innsending_endret", "Antall tilstandsendringer")
            .labelNames("tilstand", "forrigeTilstand")
            .register()
    }

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
            tilstandCounter.labels(
                event.gjeldendeTilstand.name,
                event.forrigeTilstand.name
            ).inc()
    }
}