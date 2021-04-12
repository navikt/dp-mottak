package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

private val logg = KotlinLogging.logger {}
internal class OpprettStartVedtakOppgaveMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireKey("@løsning.${Behovtype.OpprettStartVedtakOppgave.name}") }
            validate { it.requireKey("journalpostId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val arenaOppgaveOpprettetData = packet["@løsning.${Behovtype.OpprettStartVedtakOppgave.name}"].let {
            ArenaOppgaveOpprettet(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = packet["journalpostId"].asText(),
                oppgaveId = it["oppgaveId"].asText(),
                fagsakId = it["fagsakId"].asText()
            )
        }

        innsendingMediator.håndter(arenaOppgaveOpprettetData)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logg.info { problems }
    }
}
