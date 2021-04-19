package no.nav.dagpenger.mottak.behov.saksbehandling

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import java.time.LocalDate

private val logger = KotlinLogging.logger {  }

internal class EksisterendeSakerLøser(rapidsConnection: RapidsConnection) {

    init {
     EksisterendeSakerBehovLytter(rapidsConnection)
    }

    private class EksisterendeSakerBehovLytter(rapidsConnection: RapidsConnection): River.PacketListener{
        init {
            River(rapidsConnection).apply {
                validate {
                   it.demandValue("@event_name", "behov")
                    it.demandAllOrAny("@behov", listOf("EksisterendeSaker"))
                    it.rejectKey("@løsning")
                    it.requireKey("@id", "journalpostId")
                    it.requireKey("fnr")
                }
            }.register(this)
        }
        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            logger.info { "Mottok EksisterendeSaker behov for journal med id ${packet["journalpostId"].asText()}" }
            //TODO publiser til dp-quiz river
            context.publish(packet.dpQuizBehov())
        }

    }
}

private fun JsonMessage.dpQuizBehov(): String =
    JsonMessage.newMessage(
        mapOf(
            "@behov" to listOf("HarHattDagpengerSiste36Mnd"),
            "@id" to this["@id"].asText(),
            "søknad_uuid" to this["journalpostId"].asText(),
            "Virkningstidspunkt" to LocalDate.now()
        )
    ).toString()

