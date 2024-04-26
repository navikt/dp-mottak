package no.nav.dagpenger.mottak.observers

import io.prometheus.client.Counter
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
            .build()
            .namespace(DAGPENGER_NAMESPACE)
            .name("journalpost_ferdigstilt")
            .labelNames("kategorisering", "skjema", "enhet")
            .help("Number of journal post processed succesfully")
            .register()

    fun jpFerdigStillInc(
        kategorisertSom: String,
        skjemaKode: String,
        enhet: String,
    ) = jpFerdigstiltCounter
        .labels(kategorisertSom, skjemaKode, enhet)
        .inc()

    fun oppfyllerMinsteinntektArbeidsinntekt(boolean: Boolean) =
        inngangsvilkårResultatTeller
            .labels(boolean.toString())
            .inc()

    private val inngangsvilkårResultatTeller =
        Counter
            .build()
            .name("inngangsvilkaar_resultat_journalfort")
            .help("Antall søknader som oppfyller / ikke oppfyller inngangsvilkårene vi tester")
            .labelNames("oppfyller")
            .register()

    private val tilstandCounter =
        Counter
            .build("dp_innsending_endret", "Antall tilstandsendringer")
            .labelNames("tilstand", "forrigeTilstand")
            .register()

    fun tilstandCounterInc(
        gjeldendeTilstand: String,
        forrigeTilstand: String,
    ) = tilstandCounter
        .labels(gjeldendeTilstand, forrigeTilstand)
        .inc()

    private val mottakskanalCounter =
        Counter
            .build()
            .name("dp_mottak_kanal")
            .help("Antall journalposter dom dp-mottak mottar sortert på mottakskanal")
            .labelNames("mottakskanal")
            .register()

    fun mottakskanalInc(type: String) = mottakskanalCounter.labels(type).inc()
}
