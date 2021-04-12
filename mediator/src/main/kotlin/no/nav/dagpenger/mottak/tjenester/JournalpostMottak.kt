package no.nav.dagpenger.mottak.tjenester

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class JournalpostMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.requireKey("@løsning.${Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost.name}") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        TODO("not implemented")
    }
}
