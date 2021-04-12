package no.nav.dagpenger.mottak.behov

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class JournalpostBehovLøser(
    rapidsConnection: RapidsConnection,
    // journalpostclient
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

        // hente journalpost
        // legge på pakka

        // løsning på pakka

        TODO("not implemented")
    }
}
