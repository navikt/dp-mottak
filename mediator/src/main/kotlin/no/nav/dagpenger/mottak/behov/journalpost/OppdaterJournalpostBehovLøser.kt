package no.nav.dagpenger.mottak.behov.journalpost

import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.OppdaterJournalpostRequest
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class OppdaterJournalpostBehovLøser(
    private val journalpostOppdatering: JournalpostOppdatering,
    rapidsConnection: RapidsConnection
) : River.PacketListener {
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
            journalpostOppdatering.oppdaterJournalpost(journalpostId, packet.tilJournalføringOppdaterRequest())
        }
        packet["@løsning"] = mapOf("OppdaterJournalpost" to mapOf("journalpostId" to journalpostId))
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
