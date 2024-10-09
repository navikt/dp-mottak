package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging

internal class SøknadsdataBehovLøser(
    private val søknadsArkiv: SøknadsArkiv,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "behov") }
                validate { it.demandAllOrAny("@behov", listOf("Søknadsdata")) }
                validate { it.rejectKey("@løsning") }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate { it.requireKey("dokumentInfoId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val behovId = packet["@behovId"].asText()
        withMDC(
            mapOf(
                "behovId" to behovId,
                "journalpostId" to journalpostId,
            ),
        ) {
            runBlocking(MDCContext()) {
                søknadsArkiv.hentSøknadsData(packet["journalpostId"].asText(), packet["dokumentInfoId"].asText()).also {
                    packet["@løsning"] = mapOf("Søknadsdata" to it.data)
                    context.publish(packet.toJson())
                    logger.info("løser behov Søknadsdata for journalpost med id ${packet["journalpostId"].asText()}")
                }
            }
        }
    }
}
