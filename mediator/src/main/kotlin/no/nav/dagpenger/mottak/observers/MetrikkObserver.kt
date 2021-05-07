package no.nav.dagpenger.mottak.observers

import io.prometheus.client.Counter
import no.nav.dagpenger.mottak.InnsendingObserver

internal class MetrikkObserver : InnsendingObserver {

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        Metrics.tilstandCounterInc(
            event.gjeldendeTilstand.name,
            event.forrigeTilstand.name
        )
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        Metrics.jpFerdigStillInc(event.type.name)
        event.oppfyllerMinsteinntektArbeidsinntekt?.let {
            Metrics.oppfyllerMinsteinntektArbeidsinntekt(it)
        }
        if (event.fagsakId != null) {
            Metrics.automatiskJournalførtJaTellerInc(event.behandlendeEnhet)
        } else {
            Metrics.automatiskJournalførtNeiTellerInc(event.type.name, event.behandlendeEnhet)
        }
    }
}

internal object Metrics {
    private const val DAGPENGER_NAMESPACE = "dagpenger"

    private val jpFerdigstiltCounter = Counter
        .build()
        .namespace(DAGPENGER_NAMESPACE)
        .name("journalpost_ferdigstilt")
        .labelNames("kategorisering")
        .help("Number of journal post processed succesfully")
        .register()

    fun jpFerdigStillInc(kategorisertSom: String) =
        jpFerdigstiltCounter
            .labels(kategorisertSom)
            .inc()

    fun oppfyllerMinsteinntektArbeidsinntekt(boolean: Boolean) =
        inngangsvilkårResultatTeller
            .labels(boolean.toString())
            .inc()

    private val inngangsvilkårResultatTeller = Counter
        .build()
        .name("inngangsvilkaar_resultat_journalfort")
        .help("Antall søknader som oppfyller / ikke oppfyller inngangsvilkårene vi tester")
        .labelNames("oppfyller")
        .register()

    private val automatiskJournalførtTeller = Counter
        .build()
        .name("automatisk_journalfort_arena")
        .help("Antall søknader som er automatisk journalført i Arena")
        .labelNames("opprettet", "grunn", "enhet")
        .register()

    fun automatiskJournalførtJaTellerInc(enhet: String) =
        automatiskJournalførtTeller.labels("true", "arena_ok", enhet).inc()

    fun automatiskJournalførtNeiTellerInc(reason: String, enhet: String) =
        automatiskJournalførtTeller.labels("false", reason, enhet).inc()

    private val tilstandCounter = Counter
        .build("dp_innsending_endret", "Antall tilstandsendringer")
        .labelNames("tilstand", "forrigeTilstand")
        .register()

    fun tilstandCounterInc(gjeldendeTilstand: String, forrigeTilstand: String) =
        tilstandCounter
            .labels(gjeldendeTilstand, forrigeTilstand)
            .inc()
}
