package no.nav.dagpenger.mottak.behov.vilkårtester

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

class MinsteinntektVurderingSvar(rapidsConnection: RapidsConnection) : River.PacketListener {
    init {
        River(rapidsConnection).apply {
            // TODO
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        // legg svar på behov pakke og send
        TODO("Not yet implemented")
    }
}
