package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageProblems
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.JsonMessageExtensions.getOrNull
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting.Fagsystem
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveFeilet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.OppgaveOpprettet

private val logg = KotlinLogging.logger {}

internal class OppgaveOpprettetMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                precondition {
                    it.requireAll("@behov", listOf(Behovtype.OpprettOppgave.name))
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
        val løsning = packet["@løsning"].first()
        val journalpostId = packet["journalpostId"].asText()

        val fagsystem = løsning["fagsystem"].asText().let { Fagsystem.valueOf(it) }
        logg.info { "Fått løsning for ${packet["@behov"].map { it.asText() }}, journalpostId: $journalpostId. Løst av $fagsystem" }

        when (fagsystem) {
            Fagsystem.DAGPENGER -> {
                val oppgaveId = løsning["oppgaveId"].asUUID()
                val fagsakId = løsning["fagsakId"].asUUID()
                innsendingMediator.håndter(
                    OppgaveOpprettet(
                        aktivitetslogg = Aktivitetslogg(),
                        journalpostId = journalpostId,
                        oppgaveId = oppgaveId,
                        fagsakId = fagsakId,
                    ),
                )
            }
            Fagsystem.ARENA -> {
                if (løsning.has("@feil")) {
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
                            oppgaveId = løsning["oppgaveId"].asText(),
                            fagsakId = løsning.getOrNull("fagsakId")?.asText(),
                        ),
                    )
                }
            }
        }
    }

    override fun onError(
        problems: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        super.onError(problems, context, metadata)
    }

    override fun onSevere(
        error: MessageProblems.MessageException,
        context: MessageContext,
    ) {
        super.onSevere(error, context)
    }

    override fun onPreconditionError(
        error: MessageProblems,
        context: MessageContext,
        metadata: MessageMetadata,
    ) {
        super.onPreconditionError(error, context, metadata)
    }
}
