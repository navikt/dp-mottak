package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.søknadsdata.Søknadsdata
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.SøknadsdataMottak")

internal class SøknadsdataMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection
) : River.PacketListener {
    private val løsning = "@løsning.${Behovtype.Søknadsdata.name}"

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireKey(løsning) }
            validate { it.requireKey("journalpostId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val journalpostId = packet["journalpostId"].asText()

        withLoggingContext("journalpostId" to journalpostId) {
            logg.info { "Fått løsning for $løsning, journalpostId: $journalpostId" }
            val søknadsdata: Søknadsdata = packet["@løsning.${Behovtype.Søknadsdata.name}"].let { data ->
                sikkerlogg.info { packet }
                try {
                    Søknadsdata(
                        aktivitetslogg = Aktivitetslogg(),
                        journalpostId = journalpostId,
                        data = data
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
                                |  rutingoppslag=${this.javaClass.simpleName}
                                """.trimMargin()
                            }
                        }
                    }
                } catch (e: NullPointerException) {
                    sikkerlogg.error(e) { data }
                    throw e
                }
            }

            innsendingMediator.håndter(søknadsdata)
        }
    }
}
