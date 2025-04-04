package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import java.time.Duration
import java.time.LocalDateTime

internal class JournalpostBehovLøser(
    private val journalpostArkiv: JournalpostArkiv,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        private val logger = KotlinLogging.logger { }
        private val sikkerlogg = KotlinLogging.logger("tjenestekall.JournalpostBehovLøser")
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition {
                    it.requireAllOrAny(
                        "@behov",
                        listOf("Journalpost"),
                    )
                    it.forbid("@løsning")
                }
                validate { it.requireKey("@behovId", "journalpostId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val behovId = packet["@behovId"].asText()
        withMDC(
            mapOf(
                "behovId" to behovId,
                "journalpostId" to journalpostId,
            ),
        ) {
            runBlocking(MDCContext()) { journalpostArkiv.hentJournalpost(journalpostId) }.also {
                packet["@løsning"] = mapOf("Journalpost" to it)
                context.publish(packet.toJson())
                if (it.harDokumentitlerLengreEnn(255)) {
                    val dokumentTitler = it.dokumenter.joinToString { dokument -> "${dokument.tittel}\n" }
                    sikkerlogg.info { "Mottok journalpost fra Joark. Dokumentene har tittlene:\n$dokumentTitler" }
                }
                logger.info {
                    val tidSidenOpprettet =
                        it.datoOpprettet?.let { datoOpprettet ->
                            Duration.between(LocalDateTime.parse(datoOpprettet), LocalDateTime.now())
                        }
                    "Løst behov Journalpost for journalpost med id ${it.journalpostId}. Opprettet Joark for $tidSidenOpprettet siden."
                }
            }
        }
    }

    private fun SafGraphQL.Journalpost.harDokumentitlerLengreEnn(lengde: Int) = dokumenter.mapNotNull { dokument -> dokument.tittel }.any { tittel -> tittel.length > lengde }
}
