package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging

internal class FerdigstillJournalpostBehovLøser(
    private val journalpostDokarkiv: JournalpostDokarkiv,
    rapidsConnection: RapidsConnection,
) : River.PacketListener,
    JournalpostFeil {
    private companion object {
        val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection)
            .apply {
                validate { it.demandValue("@event_name", "behov") }
                validate { it.demandAllOrAny("@behov", listOf("FerdigstillJournalpost")) }
                validate { it.rejectKey("@løsning") }
                validate { it.requireKey("@behovId", "journalpostId") }
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
            try {
                runBlocking {
                    journalpostDokarkiv.ferdigstill(journalpostId, behovId)
                }
            } catch (e: JournalpostFeil.JournalpostException) {
                ignorerKjenteTilstander(e)
            }

            packet["@løsning"] =
                mapOf(
                    "FerdigstillJournalpost" to
                        mapOf(
                            "journalpostId" to journalpostId,
                        ),
                )
            context.publish(packet.toJson())
            logger.info("løste behov FerdigstillJournalpost for journalpost med id $journalpostId")
        }
    }
}
