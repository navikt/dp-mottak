package no.nav.dagpenger.mottak.observers

import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingKlient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysClient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysOppgaveRequest
import no.nav.dagpenger.mottak.meldinger.SkjemaType.Companion.tilSkjemaType
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_ORDINÆR_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_PERMITTERT_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.søknadsdata.QuizSøknadFormat
import java.time.LocalDate

private val sikkerlogg = KotlinLogging.logger("tjenestekall.FerdigstiltEttersendingObserver")

class FerdigstiltEttersendingObserver internal constructor(
    private val saksbehandlingKlient: SaksbehandlingKlient,
    private val gosysClient: GosysClient,
) : InnsendingObserver {
    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        sikkerlogg.info { "FerdigstiltEttersendingObserver innsending $event" }
        val skjemaType = event.skjemaKode.tilSkjemaType()
        if (!setOf(
                DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR_ETTERSENDING,
                DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT_ETTERSENDING,
                DAGPENGESØKNAD_ORDINÆR_ETTERSENDING,
                DAGPENGESØKNAD_PERMITTERT_ETTERSENDING,
            ).contains(skjemaType)
        ) {
            return
        }

        val søknadId: String =
            requireNotNull(
                event.søknadsData?.let {
                    QuizSøknadFormat(it).søknadsId()
                },
            )
        val ident: String = requireNotNull(event.fødselsnummer)
        val aktørId: String = requireNotNull(event.aktørId)
        runBlocking {
            when (saksbehandlingKlient.skalVarsleOmEttersending(søknadId = søknadId, ident = ident)) {
                true ->
                    runBlocking {
                        val oppgave =
                            GosysOppgaveRequest(
                                journalpostId = event.journalpostId,
                                aktoerId = aktørId,
                                tildeltEnhetsnr = event.behandlendeEnhet,
                                aktivDato = LocalDate.now(),
                                beskrivelse = "Ettersendelse til dagpengesøknad i ny løsning",
                                oppgavetype = "ETTERSEND_MOTT",
                            )
                        sikkerlogg.info { "Oppretter gosys oppgave: $oppgave for søknaId: $søknadId og ident: $ident" }
                        gosysClient.opprettOppgave(
                            oppgave =
                            oppgave,
                        ).also {
                            sikkerlogg.info { "Opprettet gosys oppgave for søknaId: $søknadId og ident: $ident og gosys id: $it " }
                        }
                    }

                else -> {}
            }
        }
    }
}
