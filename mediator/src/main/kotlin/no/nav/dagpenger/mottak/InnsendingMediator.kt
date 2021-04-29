package no.nav.dagpenger.mottak

import mu.KotlinLogging
import no.nav.dagpenger.mottak.db.InnsendingRepository
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.Eksisterendesaker
import no.nav.dagpenger.mottak.meldinger.GosysOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.MinsteinntektArbeidsinntektVurdert
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import no.nav.helse.rapids_rivers.RapidsConnection

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class InnsendingMediator(
    private val innsendingRepository: InnsendingRepository,
    private val observatører: List<InnsendingObserver> = emptyList(),
    rapidsConnection: RapidsConnection,
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

    fun håndter(journalpost: Journalpost) {
        håndter(journalpost) { innsending ->
            innsending.håndter(journalpost)
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

    fun håndter(minsteinntektVurdering: MinsteinntektArbeidsinntektVurdert) {
        håndter(minsteinntektVurdering) { innsending ->
            innsending.håndter(minsteinntektVurdering)
        }
    }

    fun håndter(eksisterendeSaker: Eksisterendesaker) {
        håndter(eksisterendeSaker) { innsending ->
            innsending.håndter(eksisterendeSaker)
        }
    }

    fun håndter(oppgaveOpprettet: GosysOppgaveOpprettet) {
        håndter(oppgaveOpprettet) { innsending ->
            innsending.håndter(oppgaveOpprettet)
        }
    }

    fun håndter(arenaOppgaveOpprettetData: ArenaOppgaveOpprettet) {
        håndter(arenaOppgaveOpprettetData) { innsending ->
            innsending.håndter(arenaOppgaveOpprettetData)
        }
    }

    fun håndter(journalpostOppdatert: JournalpostOppdatert) {
        håndter(journalpostOppdatert) { innsending ->
            innsending.håndter(journalpostOppdatert)
        }
    }

    fun håndter(journalpostFerdigstilt: JournalpostFerdigstilt) {
        håndter(journalpostFerdigstilt) { innsending ->
            innsending.håndter(journalpostFerdigstilt)
        }
    }

    fun håndter(personInformasjonIkkeFunnet: PersonInformasjonIkkeFunnet) {
        håndter(personInformasjonIkkeFunnet) { innsending ->
            innsending.håndter(personInformasjonIkkeFunnet)
        }
    }

    private fun håndter(hendelse: Hendelse, handler: (Innsending) -> Unit) {
        innsending(hendelse).also { innsending ->
            observatører.forEach { innsending.addObserver(it) }
            handler(innsending)
            finalize(innsending, hendelse)
        }
    }

    private fun innsending(hendelse: Hendelse): Innsending {
        val innsending = innsendingRepository.hent(hendelse.journalpostId())
        return when (innsending) {
            is Innsending -> {
                log.info { "Fant Innsending for ${hendelse.journalpostId()}" }
                innsending
            }
            else -> {
                val innsending = Innsending(hendelse.journalpostId())
                innsendingRepository.lagre(innsending)
                log.info { "Opprettet Innsending for ${hendelse.journalpostId()}" }
                innsending
            }
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
