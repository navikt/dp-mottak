package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import java.time.LocalDate

internal class OpprettGosysOppgaveLøser(
    private val gosysOppslag: GosysOppslag,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private companion object {
        val logger = KotlinLogging.logger { }
        val BEHOV = "OpprettGosysoppgave"
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireAllOrAny("@behov", listOf(BEHOV)) }
                precondition { it.forbid("@løsning") }
                validate { it.requireKey("@behovId", "journalpostId", "behandlendeEnhetId", "registrertDato") }
                validate { it.interestedIn("aktørId", "tilleggsinformasjon", "oppgavebeskrivelse") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val behovId = packet["@behovId"].asText()

        withMDC(
            mapOf(
                "behovId" to behovId,
                "journalpostId" to journalpostId,
            ),
        ) {
            if (listOf(
                    "598125943",
                    "598125958",
                ).contains(journalpostId) &&
                System.getenv()["NAIS_CLUSTER_NAME"] == "dev-gcp"
            ) {
                logger.warn { "Skipper journalpost" }
                return@withMDC
            }

            runBlocking(MDCContext()) {
                gosysOppslag.opprettOppgave(
                    packet.gosysOppgave(),
                )
            }.also {
                packet["@løsning"] =
                    mapOf(
                        BEHOV to
                            mapOf(
                                "journalpostId" to journalpostId,
                                "oppgaveId" to it,
                            ),
                    )
                context.publish(packet.toJson())
                logger.info { "Løste behov $BEHOV med løsning $it" }
            }
        }
    }
}

private fun JsonMessage.gosysOppgave(): GosysOppgaveRequest =
    GosysOppgaveRequest(
        journalpostId = this["journalpostId"].asText(),
        aktoerId = this["aktørId"].textValue(),
        tildeltEnhetsnr = this["behandlendeEnhetId"].asText(),
        aktivDato = LocalDate.now(),
        beskrivelse = this["oppgavebeskrivelse"].asText(),
    )
