package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.søknadsdata.Søknadsdata
import tools.jackson.databind.JsonNode

private val logg = KotlinLogging.logger {}

internal class SøknadsdataMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val løsning = "@løsning.${Behovtype.Søknadsdata.name}"

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
                validate { it.requireKey(løsning) }
                validate { it.requireKey("journalpostId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asString()
        if (journalpostId in setOf("717582885")) {
            logg.warn { "Skipper journalpostId: $journalpostId" }
            return
        }

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
                                |  avsluttedeArbeidsforhold=${avsluttetArbeidsforhold().isNotEmpty()}
                                |  rutingoppslag=${this.javaClass.simpleName}
                                |  søknadsId=${søknadId()}
                                """.trimMargin()
                            }
                        }
                    }
                }

            innsendingMediator.håndter(søknadsdata)
        }
    }
}
