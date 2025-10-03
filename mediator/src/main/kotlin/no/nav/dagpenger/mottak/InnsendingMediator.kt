package no.nav.dagpenger.mottak

import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.mottak.db.InnsendingRepository
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveFeilet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.GosysOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.OppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.dagpenger.mottak.meldinger.RekjørHendelse
import no.nav.dagpenger.mottak.meldinger.søknadsdata.Søknadsdata
import org.slf4j.MDC

private val log = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.InnsendingMediator")

internal class InnsendingMediator(
    private val innsendingRepository: InnsendingRepository,
    private val observatører: List<InnsendingObserver> = emptyList(),
    rapidsConnection: RapidsConnection,
) {
    private val behovMediator: BehovMediator =
        BehovMediator(
            rapidsConnection = rapidsConnection,
            sikkerLogg = sikkerlogg,
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

    fun håndter(oppgaveOpprettet: OppgaveOpprettet) {
        håndter(oppgaveOpprettet) { innsending ->
            innsending.håndter(oppgaveOpprettet)
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

    fun håndter(arenaOppgaveFeilet: ArenaOppgaveFeilet) {
        håndter(arenaOppgaveFeilet) { innsending ->
            innsending.håndter(arenaOppgaveFeilet)
        }
    }

    fun håndter(rekjørHendelse: RekjørHendelse) {
        håndter(rekjørHendelse) { innsending ->
            innsending.håndter(rekjørHendelse)
        }
    }

    private fun håndter(
        hendelse: Hendelse,
        handler: (Innsending) -> Unit,
    ) {
        try {
            MDC.put("journalpostId", hendelse.journalpostId())
            innsending(hendelse).also { innsending ->
                observatører.forEach { innsending.addObserver(it) }
                handler(innsending)
                finalize(innsending, hendelse)
            }
        } finally {
            MDC.clear()
        }
    }

    private fun innsending(hendelse: Hendelse): Innsending {
        val innsending = innsendingRepository.hent(hendelse.journalpostId())
        return when (innsending) {
            is Innsending -> {
                log.debug { "Fant Innsending for ${hendelse.journalpostId()}" }
                innsending
            }
            else -> {
                val nyInnsending = Innsending(hendelse.journalpostId())
                innsendingRepository.lagre(nyInnsending)
                log.info { "Opprettet Innsending for ${hendelse.journalpostId()}" }
                nyInnsending
            }
        }
    }

    private fun finalize(
        innsending: Innsending,
        hendelse: Hendelse,
    ) {
        innsendingRepository.lagre(innsending)
        if (!hendelse.hasMessages()) return
        if (hendelse.hasErrors()) return sikkerlogg.info("aktivitetslogg inneholder errors: ${hendelse.toLogString()}")
        sikkerlogg.info("aktivitetslogg inneholder meldinger: ${hendelse.toLogString()}")
        behovMediator.håndter(hendelse)
    }
}
