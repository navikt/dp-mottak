package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.OppdaterJournalpostRequest

internal class OppdaterJournalpostBehovLøser(
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
                validate { it.demandAllOrAny("@behov", listOf("OppdaterJournalpost")) }
                validate { it.rejectKey("@løsning") }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate { it.interestedIn("navn", "fødselsnummer", "tittel", "dokumenter", "fagsakId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val behovId = packet["@behovId"].asText()
        if (journalpostId in setOf("671706058")) {
            logger.warn { "Journalpost med id $journalpostId er en testjournalpost og vil ikke bli oppdatert" }
            return
        }
        withMDC(
            mapOf(
                "behovId" to behovId,
                "journalpostId" to journalpostId,
            ),
        ) {
            runBlocking {
                try {
                    journalpostDokarkiv.oppdaterJournalpost(journalpostId, packet.tilJournalføringOppdaterRequest(), behovId)
                } catch (e: JournalpostFeil.JournalpostException) {
                    ignorerKjenteTilstander(e)
                }
            }
            packet["@løsning"] = mapOf("OppdaterJournalpost" to mapOf("journalpostId" to journalpostId))
            logger.info("løser behov OppdaterJournalpost for journalpost med id $journalpostId")
            context.publish(packet.toJson())
        }
    }
}

private fun JsonMessage.tilJournalføringOppdaterRequest(): OppdaterJournalpostRequest =
    OppdaterJournalpostRequest(
        bruker = this.bruker(),
        tittel = this["tittel"].asText(),
        sak = this.sak(),
        dokumenter = this.dokumenter(),
        avsenderMottaker = this.avsender(),
    )

private fun JsonMessage.avsender(): JournalpostApi.Avsender =
    JournalpostApi.Avsender(
        id = this["fødselsnummer"].asText(),
    )

private fun JsonMessage.dokumenter(): List<JournalpostApi.Dokument> =
    this["dokumenter"].map {
        JournalpostApi.Dokument(dokumentInfoId = it["dokumentInfoId"].asText(), tittel = it["tittel"].asText())
    }

private fun JsonMessage.sak(): JournalpostApi.Sak =
    JournalpostApi.Sak(
        fagsakId = this["fagsakId"].asText(null),
    )

private fun JsonMessage.bruker(): JournalpostApi.Bruker =
    JournalpostApi.Bruker(
        id = this["fødselsnummer"].asText(),
    )
