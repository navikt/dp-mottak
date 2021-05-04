package no.nav.dagpenger.mottak

import java.time.Duration

interface InnsendingObserver {

    data class InnsendingEndretTilstandEvent(
        val journalpostId: String,
        val gjeldendeTilstand: InnsendingTilstandType,
        val forrigeTilstand: InnsendingTilstandType,
        val aktivitetslogg: Aktivitetslogg,
        val timeout: Duration
    )

    fun tilstandEndret(event: InnsendingEndretTilstandEvent) {}
}
