package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet

private val logg = KotlinLogging.logger {}

internal class PersondataMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val løsning = "@løsning.${Persondata.name}"

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
                validate { it.require(key = løsning, parser = {}) }
                validate { it.requireKey("journalpostId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
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
