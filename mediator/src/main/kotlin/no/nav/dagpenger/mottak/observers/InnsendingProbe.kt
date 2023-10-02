package no.nav.dagpenger.mottak.observers

import mu.KotlinLogging
import no.nav.dagpenger.mottak.InnsendingObserver

internal object InnsendingProbe : InnsendingObserver {
    private val log = KotlinLogging.logger { }

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        log.info {
            """Innsending ${event.journalpostId} endret tilstand 
                |fra ${event.forrigeTilstand.name} til ${event.gjeldendeTilstand.name}
            """.trimMargin()
        }
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        log.info {
            """Innsending ${event.journalpostId} ferdigstilt. 
                |Kategorisert som ${event.type}, 
                |ble sendt til behandlende enhet ${event.behandlendeEnhet}
            """.trimMargin()
        }
    }
}
