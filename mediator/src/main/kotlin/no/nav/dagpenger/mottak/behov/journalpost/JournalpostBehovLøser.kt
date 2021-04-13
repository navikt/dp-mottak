package no.nav.dagpenger.mottak.behov.journalpost

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.mottak.proxy.JournalpostArkiv
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val logger = KotlinLogging.logger { }

internal class JournalpostBehovLøser(
    private val journalpostArkiv: JournalpostArkiv,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAllOrAny("@behov", listOf("Journalpost"))
                it.rejectKey("@løsning")
                it.requireKey("@id")
                it.requireKey("journalpostId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        runBlocking { journalpostArkiv.hentJournalpost(packet["journalpostId"].asText()) }.also {
            packet["@løsning"] = mapOf("Journalpost" to it)
            context.publish(packet.toJson())
            logger.info("Løst behov Journalpost for journalpost med id ${it.journalpostId}")
        }
    }
}
