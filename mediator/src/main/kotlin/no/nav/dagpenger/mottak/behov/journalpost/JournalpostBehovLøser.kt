package no.nav.dagpenger.mottak.behov.journalpost

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC
import java.time.Duration
import java.time.LocalDateTime

internal class JournalpostBehovLøser(
    private val journalpostArkiv: JournalpostArkiv,
    rapidsConnection: RapidsConnection
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.JournalpostBehovLøser")
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("Journalpost")) }
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
            runBlocking(MDCContext()) { journalpostArkiv.hentJournalpost(journalpostId) }.also {
                packet["@løsning"] = mapOf("Journalpost" to it)
                context.publish(packet.toJson())
                if (it.harDokumentitlerLengreEnn(255)) {
                    val dokumentTitler = it.dokumenter.joinToString { dokument -> "${dokument.tittel}\n" }
                    sikkerlogg.info { "Mottok journalpost fra Joark. Dokumentene har tittlene:\n$dokumentTitler" }
                }
                logger.info {
                    val tidSidenOpprettet = it.datoOpprettet?.let { datoOpprettet ->
                        Duration.between(LocalDateTime.parse(datoOpprettet), LocalDateTime.now())
                    }
                    "Løst behov Journalpost for journalpost med id ${it.journalpostId}. Opprettet Joark for $tidSidenOpprettet siden."
                }
            }
        }
    }

    private fun SafGraphQL.Journalpost.harDokumentitlerLengreEnn(lengde: Int) =
        dokumenter.mapNotNull { dokument -> dokument.tittel }.any { tittel -> tittel.length > lengde }
}
