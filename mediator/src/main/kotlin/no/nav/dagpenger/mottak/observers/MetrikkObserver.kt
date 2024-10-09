package no.nav.dagpenger.mottak.observers

import io.prometheus.metrics.core.metrics.Counter
import no.nav.dagpenger.mottak.InnsendingObserver

internal class MetrikkObserver : InnsendingObserver {
    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        Metrics.tilstandCounterInc(
            event.gjeldendeTilstand.name,
            event.forrigeTilstand.name,
        )
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        Metrics.jpFerdigStillInc(event.type.name, event.skjemaKode, event.behandlendeEnhet)
    }
}

internal object Metrics {
    private const val DAGPENGER_NAMESPACE = "dagpenger"

    private val jpFerdigstiltCounter =
        Counter
            .builder()
            .name("${DAGPENGER_NAMESPACE}_journalpost_ferdigstilt")
            .labelNames("kategorisering", "skjema", "enhet")
            .help("Number of journal post processed succesfully")
            .register()

    fun jpFerdigStillInc(
        kategorisertSom: String,
        skjemaKode: String,
        enhet: String,
    ) = jpFerdigstiltCounter
        .labelValues(kategorisertSom, skjemaKode, enhet)
        .inc()

    private val tilstandCounter =
        Counter
            .builder()
            .name("${DAGPENGER_NAMESPACE}_dp_innsending_endret")
            .help("Antall tilstandsendringer")
            .labelNames("tilstand", "forrigeTilstand")
            .register()

    fun tilstandCounterInc(
        gjeldendeTilstand: String,
        forrigeTilstand: String,
    ) = tilstandCounter
        .labelValues(gjeldendeTilstand, forrigeTilstand)
        .inc()

    private val mottakskanalCounter =
        Counter
            .builder()
            .name("dp_mottak_kanal")
            .help("Antall journalposter dom dp-mottak mottar sortert på mottakskanal")
            .labelNames("mottakskanal")
            .register()

    fun mottakskanalInc(type: String) = mottakskanalCounter.labelValues(type).inc()
}
