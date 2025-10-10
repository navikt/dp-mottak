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
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.FagsystemBesluttet
import no.nav.dagpenger.mottak.serder.asUUID

private val logg = KotlinLogging.logger {}

internal class FagsystemMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val behovNavn = Behovtype.BestemFagsystem.name
    private val løsning = "@løsning.$behovNavn"

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                precondition { it.requireAll("@behov", listOf<String>(behovNavn)) }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
                validate { it.requireKey(løsning) }
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
        val løsningNode: JsonNode = packet[løsning]

        withLoggingContext("journalpostId" to "$journalpostId") {
            logg.info { "Mottatt løsning for behov $behovNavn med løsning: $løsningNode" }

            val fagsystemBesluttet =
                FagsystemBesluttet(
                    aktivitetslogg = Aktivitetslogg(),
                    journalpostId = journalpostId,
                    fagsystem = løsningNode.fagsystem(),
                )
            innsendingMediator.håndter(
                fagsystemBesluttet,
            )
        }
    }

    private fun JsonNode.fagsystem(): Fagsystem {
        val fagsystemType =
            try {
                Fagsystem.FagsystemType.valueOf(this["fagsystem"].asText())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Ukjent fagsystem: ${this["fagsystem"].asText()}")
            }
        return when (fagsystemType) {
            Fagsystem.FagsystemType.DAGPENGER -> Fagsystem.Dagpenger(this["fagsakId"].asUUID())
            Fagsystem.FagsystemType.ARENA -> Fagsystem.Arena
        }
    }
}
