package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.Eksisterendesaker
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

private val sikkerlogg = KotlinLogging.logger("tjenestekall.EksisterendeSakerMottak")
private val logg = KotlinLogging.logger {}

internal class EksisterendeSakerMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val løsning = "@løsning.${Behovtype.EksisterendeSaker.name}"

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireKey(løsning) }
            validate { it.requireKey("journalpostId") }
        }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        logg.info { "Fått løsning for $løsning, journalpostId: $journalpostId" }
        val eksisterendeSaker =
            Eksisterendesaker(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = journalpostId,
                harEksisterendeSak = packet[løsning]["harEksisterendeSak"].asBoolean(),
            )

        try {
            innsendingMediator.håndter(eksisterendeSaker)
        } catch (e: Exception) {
            sikkerlogg.error(e) { "Feil på mottak(jp:$journalpostId): ${packet.toJson()}" }
            throw e
        }
    }
}
