package no.nav.dagpenger.mottak.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asOptionalLocalDateTime

internal class JoarkMottak(
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
            validate { it.requireValue("temaNytt", "DAG") }
            validate { it.requireValue("hendelsesType", "MidlertidigJournalført") }
            validate {
                it.require("mottaksKanal") { json ->
                    if (json.asText() == "EESSI") throw IllegalArgumentException("Kan ikke håndtere 'EESSI' mottakskanal")
                }
            }
            validate { it.interestedIn("temaNytt", "hendelsesType", "mottaksKanal", "behandlingsTema", "timestamp") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        logg.info(
            """Received journalpost with journalpost id: ${packet["journalpostId"].asText()} 
                        |tema: ${packet["temaNytt"].asText()}, 
                        |hendelsesType: ${packet["hendelsesType"].asText()}, 
                        |mottaksKanal, ${packet["mottaksKanal"].asText()}, 
                        |behandlingsTema: ${packet["behandlingsTema"].asText()},
                        |produsert: ${packet["timestamp"].asOptionalLocalDateTime()}
                        |""".trimMargin()
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
