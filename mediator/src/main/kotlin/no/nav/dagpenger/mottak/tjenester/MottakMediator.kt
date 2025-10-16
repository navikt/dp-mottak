package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import no.nav.dagpenger.mottak.InnsendingMediator

internal class MottakMediator(
    mediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) {
    init {
        JoarkMottak(mediator, rapidsConnection)
        JournalpostMottak(mediator, rapidsConnection)
        PersondataMottak(mediator, rapidsConnection)
        FagsystemMottak(mediator, rapidsConnection)
        JournalpostOppdatertMottak(mediator, rapidsConnection)
        JournalpostFerdigstiltMottak(mediator, rapidsConnection)
        OpprettArenaOppgaveMottak(mediator, rapidsConnection)
        SøknadsdataMottak(mediator, rapidsConnection)
        GosysOppgaveOpprettetMottak(mediator, rapidsConnection)
        DagpengerOppgaveOpprettetMottak(mediator, rapidsConnection)
        RekjørMottak(mediator, rapidsConnection)
    }
}
