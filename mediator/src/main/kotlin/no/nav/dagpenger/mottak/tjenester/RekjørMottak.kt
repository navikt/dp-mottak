package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.RekjørHendelse

internal class RekjørMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val logg = KotlinLogging.logger {}

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "rekjør_innsending") }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
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
        logg.info { "Fått rekjøringshendelse for journalpostId: $journalpostId" }
        val rekjørHendelse =
            RekjørHendelse(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = journalpostId,
            )

        innsendingMediator.håndter(rekjørHendelse)
    }
}
