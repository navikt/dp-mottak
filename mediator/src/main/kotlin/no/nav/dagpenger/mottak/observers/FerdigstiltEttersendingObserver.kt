package no.nav.dagpenger.mottak.observers

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysClient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysOppgaveRequest
import no.nav.dagpenger.mottak.meldinger.SkjemaType
import no.nav.dagpenger.mottak.meldinger.SkjemaType.Companion.tilSkjemaType
import no.nav.dagpenger.mottak.meldinger.søknadsdata.QuizSøknadFormat
import no.nav.dagpenger.mottak.tjenester.SaksbehandlingHttpKlient
import java.time.LocalDate

private val sikkerlogg = KotlinLogging.logger("tjenestekall.FerdigstiltEttersendingObserver")

class FerdigstiltEttersendingObserver internal constructor(
    private val saksbehandlingKlient: SaksbehandlingHttpKlient,
    private val gosysClient: GosysClient,
) : InnsendingObserver {
    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        val skjemaType = event.skjemaKode.tilSkjemaType()
        if (skjemaType != SkjemaType.DAGPENGESØKNAD_ORDINÆR_ETTERSENDING && skjemaType != SkjemaType.DAGPENGESØKNAD_PERMITTERT_ETTERSENDING) {
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
                        gosysClient.opprettOppgave(
                            oppgave =
                                GosysOppgaveRequest(
                                    journalpostId = event.journalpostId,
                                    aktoerId = aktørId,
                                    // TODO: Skal denne settes til en ny enhet?
                                    tildeltEnhetsnr = event.behandlendeEnhet,
                                    aktivDato = LocalDate.now(),
                                    beskrivelse = "Ettersending til dagpengesøknad i ny løsning",
                                ),
                        )
                    }
                else -> {}
            }
        }
    }
}
