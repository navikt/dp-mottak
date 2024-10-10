package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold.Sluttårsak.KONTRAKT_UTGAATT
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.søknadsdata.Søknadsdata

private val logg = KotlinLogging.logger {}

internal class SøknadsdataMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val løsning = "@løsning.${Behovtype.Søknadsdata.name}"

    init {
        River(rapidsConnection)
            .apply {
                validate { it.requireValue("@event_name", "behov") }
                validate { it.requireValue("@final", true) }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
                validate { it.requireKey(løsning) }
                validate { it.requireKey("journalpostId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val journalpostId = packet["journalpostId"].asText()

        withLoggingContext("journalpostId" to journalpostId) {
            logg.info { "Fått løsning for $løsning, journalpostId: $journalpostId" }
            val søknadsdata: Søknadsdata =
                packet["@løsning.${Behovtype.Søknadsdata.name}"].let { data ->
                    Søknadsdata(
                        aktivitetslogg = Aktivitetslogg(),
                        journalpostId = journalpostId,
                        data = data,
                    ).also { søknadsdata ->
                        with(søknadsdata.søknad()) {
                            logg.info {
                                """Søknadsdata sier:
                                |  konkurs=${avsluttetArbeidsforholdFraKonkurs()}
                                |  eøsBostedsland=${eøsBostedsland()}
                                |  eøsArbeidsforhold=${eøsArbeidsforhold()}
                                |  harAvtjentVerneplikt=${avtjentVerneplikt()}
                                |  erPermittertFraFiskeforedling=${permittertFraFiskeForedling()}
                                |  erPermittert=${permittert()}
                                |  harBarn=${harBarn()}
                                |  harAndreYtelser=${harAndreYtelser()}
                                |  avsluttedeArbeidsforhold=${avsluttetArbeidsforhold().isEmpty()}
                                |  rutingoppslag=${this.javaClass.simpleName}
                                """.trimMargin()

                                if (!avtjentVerneplikt() &&
                                    !harBarn() &&
                                    !eøsArbeidsforhold() &&
                                    !eøsBostedsland() &&
                                    !harAndreYtelser() &&
                                    avsluttetArbeidsforhold().size == 1 &&
                                    (
                                        avsluttetArbeidsforhold().single().sluttårsak == SAGT_OPP_AV_ARBEIDSGIVER ||
                                            avsluttetArbeidsforhold().single().sluttårsak == KONTRAKT_UTGAATT
                                    )
                                ) {
                                    logg.info { "Søknad er en mulig case for innvilgelse." }
                                }
                            }
                        }
                    }
                }

            innsendingMediator.håndter(søknadsdata)
        }
    }
}
