package no.nav.dagpenger.mottak.behov.person

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging

internal class PersondataBehovLøser(
    private val personOppslag: PersonOppslag,
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
                    it.requireAllOrAny("@behov", listOf("Persondata"))
                    it.forbid("@løsning")
                }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate { it.requireKey("brukerId") }
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
            runBlocking(MDCContext()) { personOppslag.hentPerson(packet["brukerId"].asText()) }.also {
                packet["@løsning"] = mapOf("Persondata" to it)
                context.publish(packet.toJson())
                logger.info("Løst behov Persondata for journalpost med id ${packet["journalpostId"].asText()}")
            }
        }
    }
}
