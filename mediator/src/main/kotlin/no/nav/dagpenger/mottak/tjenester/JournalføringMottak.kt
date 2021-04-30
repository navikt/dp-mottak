package no.nav.dagpenger.mottak.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class JournalføringMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    private companion object {
        private val logg = KotlinLogging.logger {}
    }

    init {
        River(rapidsConnection).apply {
            validate { it.requireKey("journalpostId") }
            validate { it.requireKey("journalpostStatus") }
            validate { it.interestedIn("temaNytt", "hendelsesType", "mottaksKanal", "behandlingstema") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        logg.info(
            """Received journalpost with journalpost id: ${packet["journalpostId"].asText()} 
                        |tema: ${packet["temaNytt"].asText()}, 
                        |hendelsesType: ${packet["hendelsesType"].asText()}, 
                        |mottakskanal, ${packet["mottaksKanal"].asText()}, 
                        |behandlingstema: ${packet["behandlingstema"].asText()}""".trimMargin()
        )

        val joarkHendelse = JoarkHendelse(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = packet["journalpostId"].asText(),
            hendelseType = packet["hendelsesType"].asText(),
            journalpostStatus = packet["journalpostStatus"].asText(),
            behandlingstema = packet["behandlingstema"].asText() ?: null
        )

        innsendingMediator.håndter(joarkHendelse)
    }
}
