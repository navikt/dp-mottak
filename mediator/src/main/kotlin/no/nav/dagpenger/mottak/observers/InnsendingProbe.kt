package no.nav.dagpenger.mottak.observers

import mu.KotlinLogging
import no.nav.dagpenger.mottak.InnsendingObserver

internal object InnsendingProbe : InnsendingObserver {

    private val log = KotlinLogging.logger { }

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        log.info {
            "Innsending ${event.journalpostId} endret tilstand fra ${event.forrigeTilstand.name} til ${event.gjeldendeTilstand.name}"
        }
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        log.info {
            "Innsending ${event.journalpostId} ferdigstilt. Ble sendt til behandlende enhet ${event.behandlendeEnhet}"
        }
    }
}
