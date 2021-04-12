package no.nav.dagpenger.mottak

import mu.KotlinLogging
import no.nav.dagpenger.mottak.db.InnsendingRepository
import no.nav.dagpenger.mottak.meldinger.EksisterendesakData
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.MinsteinntektVurderingData
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import no.nav.helse.rapids_rivers.RapidsConnection

private val logg = KotlinLogging.logger {}
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

    fun håndter(journalpostData: JournalpostData) {
        håndter(journalpostData) { innsending ->
            innsending.håndter(journalpostData)
        }
    }

    fun håndter(persondata: PersonInformasjon) {
        håndter(persondata) { innsending ->
            innsending.håndter(persondata)
        }
    }

    fun håndter(søknadsdata: Søknadsdata) {
        håndter(søknadsdata) { innsending ->
            innsending.håndter(søknadsdata)
        }
    }

    fun håndter(minsteinntektVurdering: MinsteinntektVurderingData) {
        håndter(minsteinntektVurdering) { innsending ->
            innsending.håndter(minsteinntektVurdering)
        }
    }

    fun håndter(eksisterendeSaker: EksisterendesakData) {
        håndter(eksisterendeSaker) { innsending ->
            innsending.håndter(eksisterendeSaker)
        }
    }

    private fun håndter(hendelse: Hendelse, handler: (Innsending) -> Unit) {
        innsendingRepository.innsending(hendelse.journalpostId()).also { innsending ->
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
