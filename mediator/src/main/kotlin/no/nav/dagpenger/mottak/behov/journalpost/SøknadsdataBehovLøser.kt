package no.nav.dagpenger.mottak.behov.journalpost

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class SøknadsdataBehovLøser(
    private val søknadsArkiv: SøknadsArkiv,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAllOrAny("@behov", listOf("Søknadsdata"))
                it.rejectKey("@løsning")
                it.requireKey("@id", "journalpostId")
                it.requireKey("dokumentInfoId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            runBlocking {
                søknadsArkiv.hentSøknadsData(packet["journalpostId"].asText(), packet["dokumentInfoId"].asText()).also {
                    packet["@løsning"] = mapOf("Søknadsdata" to it.data)
                    context.publish(packet.toJson())
                    logger.info("løser behov Søknadsdata for journalpost med id ${packet["journalpostId"].asText()}")
                }
            }
        } catch (e: Exception) {
            logger.error(e) { "Klarte ikke å hente søknadsdata" }
        }
    }
}
