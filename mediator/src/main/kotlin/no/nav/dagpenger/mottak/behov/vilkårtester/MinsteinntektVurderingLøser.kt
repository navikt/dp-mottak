package no.nav.dagpenger.mottak.behov.vilkårtester

import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class MinsteinntektVurderingLøser(
    rapidsConnection: RapidsConnection,
    private val regelApiClient: RegelApiClient
) : River.PacketListener {
    private companion object {
        val logger = KotlinLogging.logger { }
    }

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

        try {
            regelApiClient.startMinsteinntektVurdering(
                aktørId = packet["aktørId"].asText(),
                journalpostId = packet["journalpostId"].asText()
            )
        } catch (e: Exception) {
            logger.warn(e) { "Feil ved start av minsteinntekts vurdering for journalpost med id ${packet["journalpostId"]}" }
            packet["@løsning"] = mapOf("MinsteinntektVurdering" to null)
            context.publish(packet.toJson())
        }
    }
}
