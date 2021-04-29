package no.nav.dagpenger.mottak.behov.journalpost

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.OppdaterJournalpostRequest
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class OppdaterJournalpostBehovLøser(
    private val journalpostDokarkiv: JournalpostDokarkiv,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("OppdaterJournalpost")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@id", "journalpostId") }
            validate { it.interestedIn("fødselsnummer", "tittel", "dokumenter", "fagsakId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val journalpostId = packet["journalpostId"].asText()
        runBlocking {
            journalpostDokarkiv.oppdaterJournalpost(journalpostId, packet.tilJournalføringOppdaterRequest())
        }
        packet["@løsning"] = mapOf("OppdaterJournalpost" to mapOf("journalpostId" to journalpostId))
        logger.info("løser behov OppdaterJournalpost for journalpost med id $journalpostId")
        context.publish(packet.toJson())
    }
}

private fun JsonMessage.tilJournalføringOppdaterRequest(): OppdaterJournalpostRequest =
    OppdaterJournalpostRequest(
        bruker = this.bruker(),
        tittel = this["tittel"].asText(),
        sak = this.sak(),
        dokumenter = this.dokumenter(),
    )

private fun JsonMessage.dokumenter(): List<JournalpostApi.Dokument> =
    this["dokumenter"].map {
        JournalpostApi.Dokument(dokumentInfoId = it["dokumentInfoId"].asText(), tittel = it["tittel"].asText())
    }

private fun JsonMessage.sak(): JournalpostApi.Sak =
    JournalpostApi.Sak(
        fagsakId = this["fagsakId"].asText(null)
    )

private fun JsonMessage.bruker(): JournalpostApi.Bruker =
    JournalpostApi.Bruker(
        id = this["fødselsnummer"].asText()
    )
