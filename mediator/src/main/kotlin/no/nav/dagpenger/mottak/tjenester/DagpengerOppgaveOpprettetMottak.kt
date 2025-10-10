package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.DagpengerOppgaveOpprettet
import no.nav.dagpenger.mottak.serder.asUUID

private val logg = KotlinLogging.logger {}

class DagpengerOppgaveOpprettetMottak internal constructor(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val behovNavn = Behovtype.OpprettDagpengerOppgave.name
    private val løsning = "@løsning.$behovNavn"

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                precondition { it.requireAll("@behov", listOf<String>(behovNavn)) }
                validate {
                    it.require("@opprettet", JsonNode::asLocalDateTime)
                    it.requireKey(løsning)
                    it.requireKey("journalpostId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val løsningNode = packet[løsning]

        withLoggingContext("journalpostId" to "$journalpostId") {
            logg.info { "Mottatt løsning for behov $behovNavn med løsning: $løsningNode" }
            val dagpengerOppgaveOpprettet =
                DagpengerOppgaveOpprettet(
                    aktivitetslogg = Aktivitetslogg(),
                    journalpostId = journalpostId,
                    oppgaveId = løsningNode["oppgaveId"].asUUID(),
                    fagsakId = løsningNode["fagsakId"].asUUID(),
                )
            innsendingMediator.håndter(
                dagpengerOppgaveOpprettet,
            )
        }
    }
}
