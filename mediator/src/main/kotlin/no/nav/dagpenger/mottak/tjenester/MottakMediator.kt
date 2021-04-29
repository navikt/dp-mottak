package no.nav.dagpenger.mottak.tjenester

import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.helse.rapids_rivers.RapidsConnection

internal class MottakMediator(mediator: InnsendingMediator, rapidsConnection: RapidsConnection) {
    init {
        JournalføringMottak(mediator, rapidsConnection)
        JournalpostMottak(mediator, rapidsConnection)
        EksisterendeSakerMottak(mediator, rapidsConnection)
        PersondataMottak(mediator, rapidsConnection)
        JournalpostOppdatertMottak(mediator, rapidsConnection)
        JournalpostFerdigstiltMottak(mediator, rapidsConnection)
        MinsteinntektVurderingMottak(mediator, rapidsConnection)
        OpprettArenaOppgaveMottak(mediator, rapidsConnection)
        SøknadsdataMottak(mediator, rapidsConnection)
        GosysOppgaveOpprettetMottak(mediator, rapidsConnection)
    }
}
