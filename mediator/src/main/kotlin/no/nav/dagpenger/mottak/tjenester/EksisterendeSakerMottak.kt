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

private val logg = KotlinLogging.logger {}
internal class EksisterendeSakerMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection
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

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        logg.info { "Fått løsning for $løsning, journalpostId: ${packet["journalpostId"]}" }
        val eksisterendeSaker = Eksisterendesaker(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = packet["journalpostId"].asText(),
            harEksisterendeSak = packet[løsning]["harEksisterendeSak"].asBoolean()
        )

        innsendingMediator.håndter(eksisterendeSaker)
    }
}
