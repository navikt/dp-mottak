package no.nav.dagpenger.mottak.behov.journalpost

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC

internal class FerdigstillJournalpostBehovLøser(
    private val journalpostDokarkiv: JournalpostDokarkiv,
    rapidsConnection: RapidsConnection,
) : River.PacketListener, JournalpostFeil {

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("FerdigstillJournalpost")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@behovId", "journalpostId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val journalpostId = packet["journalpostId"].asText()
        val behovId = packet["@behovId"].asText()

        withMDC(
            mapOf(
                "behovId" to behovId,
                "journalpostId" to journalpostId
            )
        ) {
            try {
                runBlocking {
                    journalpostDokarkiv.ferdigstill(journalpostId)
                }
            } catch (e: JournalpostFeil.JournalpostException) {
                ignorerKjenteTilstander(e)
            }

            packet["@løsning"] = mapOf(
                "FerdigstillJournalpost" to mapOf(
                    "journalpostId" to journalpostId
                )
            )
            context.publish(packet.toJson())
            logger.info("løste behov FerdigstillJournalpost for journalpost med id $journalpostId")
        }
    }
}
