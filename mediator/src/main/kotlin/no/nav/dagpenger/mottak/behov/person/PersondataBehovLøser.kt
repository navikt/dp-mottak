package no.nav.dagpenger.mottak.behov.person

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC

internal class PersondataBehovLøser(
    private val personOppslag: PersonOppslag,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("Persondata")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@behovId", "journalpostId") }
            validate { it.requireKey("brukerId") }
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
            runBlocking(MDCContext()) { personOppslag.hentPerson(packet["brukerId"].asText()) }.also {
                packet["@løsning"] = mapOf("Persondata" to it)
                context.publish(packet.toJson())
                logger.info("Løst behov Persondata for journalpost med id ${packet["journalpostId"].asText()}")
            }
        }
    }
}
