package no.nav.dagpenger.mottak.behov.vilkårtester

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class MinsteinntektVurderingLøser(
    private val regelApiClient: RegelApiClient,
    rapidsConnection: RapidsConnection
) {

    init {
        StartBehovPacketListener(regelApiClient, rapidsConnection)
        LøsningPacketListener(rapidsConnection)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
        val pendingPackets = mutableMapOf<String, JsonMessage>()
    }

    private class StartBehovPacketListener(private val regelApiClient: RegelApiClient, rapidsConnection: RapidsConnection) :
        River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandAllOrAny("@behov", listOf("MinsteinntektVurdering"))
                    it.rejectKey("@løsning")
                    it.requireKey("@id", "journalpostId")
                    it.requireKey("aktørId")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {

            runBlocking {
                val journalpostId = packet["journalpostId"].asText()
                try {
                    logger.info { "Forsøker å opprette minsteinntektvurderingsbehov i regel-api for journalpost med ${packet["journalpostId"]}" }
                    regelApiClient.startMinsteinntektVurdering(
                        aktørId = packet["aktørId"].asText(),
                        journalpostId = journalpostId
                    )
                    pendingPackets.putIfAbsent(journalpostId, packet)
                } catch (e: Exception) {
                    logger.warn(e) { "Feil ved start av minsteinntekts vurdering for journalpost med id ${packet["journalpostId"]}" }
                    packet["@løsning"] = mapOf("MinsteinntektVurdering" to null)
                    context.publish(packet.toJson())
                }
            }
        }
    }

    private class LøsningPacketListener(rapidsConnection: RapidsConnection) :
        River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("kontekstType", "soknad")
                    it.requireKey("kontekstId")
                    it.requireKey("minsteinntektResultat")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            pendingPackets.remove(packet["kontekstId"].asText())?.let {
                it["@løsning"] = mapOf("MinsteinntektVurdering" to MinsteinntektVurdering(packet["minsteinntektResultat"]["oppfyllerMinsteinntekt"].asBoolean()))
                context.publish(it.toJson())
                logger.info { "Løste behove for minsteinntekt ${packet["kontekstId"].asText()}" }
            }
        }

        private data class MinsteinntektVurdering(val oppfyllerMinsteArbeidsinntekt: Boolean)
    }
}
