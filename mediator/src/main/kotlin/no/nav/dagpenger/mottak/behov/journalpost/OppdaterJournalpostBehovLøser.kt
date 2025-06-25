package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
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
                precondition {
                    it.requireValue("@event_name", "behov")
                    it.requireAllOrAny("@behov", listOf("OppdaterJournalpost"))
                    it.forbid("@løsning")
                }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate { it.interestedIn("navn", "fødselsnummer", "tittel", "dokumenter", "fagsakId", "mottakskanal") }
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
        if (emptyList<String>().contains(journalpostId)) {
            logger.warn { "Skipper journalpost $journalpostId fra OppdaterJournalpostBehovLøser" }
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
            logger.info("løser behov OppdaterJournalpost (mottakskanal ${packet.mottakskanal()})")
            context.publish(packet.toJson())
        }
    }
}

/***
 * journalpostapi oppdaterJournalpost tillater ikke at avsender overskrives hvis journalpost er sendt inn digitalt
 *
 * Berørte tjenester/apper: dokarkiv journalpostapi (PUT oppdaterJournalpost)
 * Miljø: Alle testmiljøer og produksjon
 * Digitale innsendinger har god kvalitet på metadata om avsender. Avsender overskrives likevel av noen fagsystemer / fra noen arbeidsflater.
 * Forsøk på å oppdatere avsender på en digitalt innsendt journalpost med en av kanalene NAV_NO, NAV_NO_CHAT, ALTINN, EESSI vil avvises med HTTP-status 400 Bad Request
 *
 */
private fun JsonMessage.oppdatereAvsender() =
    when (mottakskanal()) {
        "NAV_NO" -> false
        else -> true
    }

private fun JsonMessage.mottakskanal(): String? = this["mottakskanal"].asText("ukjent")

private fun JsonMessage.avsender(): JournalpostApi.Avsender? =
    if (oppdatereAvsender()) {
        JournalpostApi.Avsender(
            id = this["fødselsnummer"].asText(),
        )
    } else {
        null
    }

private fun JsonMessage.tilJournalføringOppdaterRequest(): OppdaterJournalpostRequest =
    OppdaterJournalpostRequest(
        bruker = this.bruker(),
        tittel = this["tittel"].asText(),
        sak = this.sak(),
        dokumenter = this.dokumenter(),
        avsenderMottaker = this.avsender(),
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
