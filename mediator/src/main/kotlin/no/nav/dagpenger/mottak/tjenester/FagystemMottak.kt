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
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.System
import no.nav.dagpenger.mottak.meldinger.FagsystemBesluttet

private val logg = KotlinLogging.logger {}

internal class FagystemMottak(
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
                // TODO: Skal denne være required? Eller må vi håndtere at behovet er løst med @feil?
                validate { it.requireKey("fagsystem") }
                validate { it.interestedIn("fagsakId") }
            }.register(this)
    }

    private fun JsonMessage.fagsystem(): System {
        val fagsystem =
            try {
                System.Fagsystem.valueOf(this["fagsystem"].asText())
            } catch (e: IllegalArgumentException) {
                throw IllegalArgumentException("Ukjent fagsystem: ${this["fagsystem"].asText()}")
            }
        return when (fagsystem) {
            System.Fagsystem.DAGPENGER -> System.Dagpenger(this["fagsakId"].asUUID())
            System.Fagsystem.ARENA -> System.Arena
        }
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val løsning = packet["@løsning"].first()
        val journalpostId = packet["journalpostId"].asText()
        val fagsystem = packet["fagsystem"].asText()
        logg.info { "Fått løsning for ${packet["@behov"].map { it.asText() }}, journalpostId: $journalpostId. Fagsystem er $fagsystem" }

        val fagsystemBesluttet =
            FagsystemBesluttet(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = journalpostId,
                system = packet.fagsystem(),
            )

        innsendingMediator.håndter(fagsystemBesluttet)

//        val fagsystem = løsning["fagsystem"].asText().let { FagSystem.valueOf(it) }
//        logg.info { "Fått løsning for ${packet["@behov"].map { it.asText() }}, journalpostId: $journalpostId. Løst av $fagsystem" }
//
//        when (fagsystem) {
//            FagSystem.DAGPENGER -> {
//                val oppgaveId = løsning["oppgaveId"].asUUID()
//                val fagsakId = løsning["fagsakId"].asUUID()
//                innsendingMediator.håndter(
//                    OppgaveOpprettet(
//                        aktivitetslogg = Aktivitetslogg(),
//                        journalpostId = journalpostId,
//                        oppgaveId = oppgaveId,
//                        fagsakId = fagsakId,
//                    ),
//                )
//            }
//            FagSystem.ARENA -> {
//                if (løsning.has("@feil")) {
//                    innsendingMediator.håndter(
//                        ArenaOppgaveFeilet(
//                            aktivitetslogg = Aktivitetslogg(),
//                            journalpostId = packet["journalpostId"].asText(),
//                        ),
//                    )
//                } else {
//                    innsendingMediator.håndter(
//                        ArenaOppgaveOpprettet(
//                            aktivitetslogg = Aktivitetslogg(),
//                            journalpostId = journalpostId,
//                            oppgaveId = løsning["oppgaveId"].asText(),
//                            fagsakId = løsning.getOrNull("fagsakId")?.asText(),
//                        ),
//                    )
//                }
//            }
//        }
    }
}
