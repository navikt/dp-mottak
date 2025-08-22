package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext

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
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", listOf("Søknadsdata"))
                    it.forbid("@løsning")
                }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate { it.requireKey("dokumentInfoId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
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
