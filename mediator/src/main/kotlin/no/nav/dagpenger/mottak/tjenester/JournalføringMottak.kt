package no.nav.dagpenger.mottak.tjenester

import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall")

internal class JournalføringMottak(
    rapidsConnection: RapidsConnection,
    private val innsendingMediator: InnsendingMediator
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate(River.PacketValidation { it.requireKey("journalpostId") })
            validate(River.PacketValidation { it.requireKey("journalpostStatus") })
            validate(River.PacketValidation { it.interestedIn("temaNytt", "hendelsesType", "mottaksKanal", "behandlingstema") })
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        logg.info(
            """Received journalpost with journalpost id: ${packet["journalpostId"].asText()} 
                        |tema: ${packet["temaNytt"]}, 
                        |hendelsesType: ${packet["hendelsesType"]}, 
                        |mottakskanal, ${packet["mottaksKanal"]}, 
                        |behandlingstema: ${packet["behandlingstema"]}""".trimMargin()
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

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logg.error { problems }
    }
}
