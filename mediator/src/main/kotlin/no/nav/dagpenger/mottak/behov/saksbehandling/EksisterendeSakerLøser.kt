package no.nav.dagpenger.mottak.behov.saksbehandling

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River


internal class EksisterendeSakerLøser(
    private val arenaOppslag: ArenaOppslag,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    companion object {
        private val logger = KotlinLogging.logger { }
    }

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
        try {
            runBlocking {
                arenaOppslag.harEksisterendeSaker(packet["fnr"].asText()).also {
                    packet["@løsning"] = mapOf("EksisterendeSaker" to mapOf("harEksisterendeSak" to it))
                    context.publish(packet.toJson())
                }
            }
        } catch (e: Exception) {
            logger.info { "Kunne ikke hente eksisterende saker for søknad med journalpostId ${packet["journalpostId"]}" }
            throw e
        }
    }
}
