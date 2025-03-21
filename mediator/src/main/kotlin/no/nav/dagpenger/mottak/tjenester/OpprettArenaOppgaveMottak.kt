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
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.JsonMessageExtensions.getOrNull
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveFeilet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet

private val logg = KotlinLogging.logger {}

internal class OpprettArenaOppgaveMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                precondition {
                    it.requireAllOrAny(
                        "@behov",
                        listOf(Behovtype.OpprettStartVedtakOppgave.name, Behovtype.OpprettVurderhenvendelseOppgave.name),
                    )
                }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
                validate { it.requireKey("@løsning") }
                validate { it.requireKey("journalpostId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val arenaLøsning = packet["@løsning"].first()
        val journalpostId = packet["journalpostId"].asText()
        logg.info { "Fått løsning for ${packet["@behov"].map { it.asText() }}, journalpostId: $journalpostId" }
        if (arenaLøsning.has("@feil")) {
            innsendingMediator.håndter(
                ArenaOppgaveFeilet(
                    aktivitetslogg = Aktivitetslogg(),
                    journalpostId = packet["journalpostId"].asText(),
                ),
            )
        } else {
            innsendingMediator.håndter(
                ArenaOppgaveOpprettet(
                    aktivitetslogg = Aktivitetslogg(),
                    journalpostId = journalpostId,
                    oppgaveId = arenaLøsning["oppgaveId"].asText(),
                    fagsakId = arenaLøsning.getOrNull("fagsakId")?.asText(),
                ),
            )
        }
    }
}
