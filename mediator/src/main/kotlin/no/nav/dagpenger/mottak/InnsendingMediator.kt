package no.nav.dagpenger.mottak

import mu.KotlinLogging
import no.nav.dagpenger.mottak.db.InnsendingRepository
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.helse.rapids_rivers.RapidsConnection

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class InnsendingMediator(
    private val innsendingRepository: InnsendingRepository,
    rapidsConnection: RapidsConnection
) {

    private val behovMediator: BehovMediator = BehovMediator(
        rapidsConnection = rapidsConnection,
        sikkerLogg = sikkerlogg
    )

    fun håndter(joarkHendelse: JoarkHendelse) {
        håndter(joarkHendelse) { innsending ->
            innsending.håndter(joarkHendelse)
        }
    }

    private fun håndter(hendelse: Hendelse, handler: (Innsending) -> Unit) {
        innsendingRepository.person(hendelse.journalpostId()).also { innsending ->
            handler(innsending)
            finalize(innsending, hendelse)
        }
    }

    private fun finalize(innsending: Innsending, hendelse: Hendelse) {
        innsendingRepository.lagre(innsending)
        if (!hendelse.hasMessages()) return
        if (hendelse.hasErrors()) return sikkerlogg.info("aktivitetslogg inneholder errors: ${hendelse.toLogString()}")
        sikkerlogg.info("aktivitetslogg inneholder meldinger: ${hendelse.toLogString()}")
        behovMediator.håndter(hendelse)
    }
}
