package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

private val logg = KotlinLogging.logger {}

internal class PersondataMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {

    private val løsning = "@løsning.${Persondata.name}"

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.require(key = løsning, parser = {}) }
            validate { it.requireKey("journalpostId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val journalpostId = packet["journalpostId"].asText()
        logg.info { "Fått løsning for $løsning, journalpostId: $journalpostId" }
        val persondata = packet[løsning]
        if (persondata.isNull) {
            innsendingMediator.håndter(PersonInformasjonIkkeFunnet(Aktivitetslogg(), journalpostId))
        } else {
            PersonInformasjon(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = journalpostId,
                aktørId = persondata["aktørId"].asText(),
                ident = persondata["fødselsnummer"].asText(),
                diskresjonskode = persondata["diskresjonskode"].textValue(),
                navn = persondata["navn"].asText(),
                norskTilknytning = persondata["norskTilknytning"].asBoolean(),
                egenAnsatt = persondata.get("egenAnsatt")?.asBoolean() ?: false,
            ).also { innsendingMediator.håndter(it) }
        }
    }
}
