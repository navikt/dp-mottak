package no.nav.dagpenger.mottak.behov.vilkårtester

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class MinsteinntektVurderingLøser(
    regelApiClient: RegelApiClient,
    private val repository: MinsteinntektVurderingRepository,
    rapidsConnection: RapidsConnection
) {

    init {
        StartBehovPacketListener(regelApiClient, rapidsConnection)
        LøsningPacketListener(rapidsConnection)
    }

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    private inner class StartBehovPacketListener(
        private val regelApiClient: RegelApiClient,
        rapidsConnection: RapidsConnection
    ) :
        River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate { it.demandValue("@event_name", "behov") }
                validate { it.demandAllOrAny("@behov", listOf("MinsteinntektVurdering")) }
                validate { it.rejectKey("@løsning") }
                validate { it.requireKey("@id", "journalpostId") }
                validate { it.requireKey("aktørId") }
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
                    repository.lagre(journalpostId, packet)
                } catch (e: Exception) {
                    logger.warn(e) { "Feil ved start av minsteinntekts vurdering for journalpost med id ${packet["journalpostId"]}" }
                    packet["@løsning"] = mapOf("MinsteinntektVurdering" to null)
                    context.publish(packet.toJson())
                }
            }
        }
    }

    private inner class LøsningPacketListener(
        rapidsConnection: RapidsConnection
    ) :
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
            val key = repository.fjern(packet["kontekstId"].asText())
            key?.let {
                it["@løsning"] =
                    mapOf("MinsteinntektVurdering" to MinsteinntektVurdering(packet["minsteinntektResultat"]["oppfyllerMinsteinntekt"].asBoolean()))
                context.publish(it.toJson())
                logger.info { "Løste behov for minsteinntekt ${packet["kontekstId"].asText()}" }
            }
        }
    }

    private data class MinsteinntektVurdering(val oppfyllerMinsteArbeidsinntekt: Boolean)
}

interface MinsteinntektVurderingRepository {
    fun lagre(journalpostId: String, packet: JsonMessage): Int
    fun fjern(journalpostId: String): JsonMessage?
}
