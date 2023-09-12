package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC
import java.time.LocalDate

internal class OpprettGosysOppgaveLøser(private val gosysOppslag: GosysOppslag, rapidsConnection: RapidsConnection) :
    River.PacketListener {
    private companion object {
        val logger = KotlinLogging.logger { }
        val BEHOV = "OpprettGosysoppgave"
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf(BEHOV)) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@behovId", "journalpostId", "behandlendeEnhetId", "registrertDato") }
            validate { it.interestedIn("aktørId", "tilleggsinformasjon", "oppgavebeskrivelse") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val journalpostId = packet["journalpostId"].asText()
        val behovId = packet["@behovId"].asText()

        withMDC(
            mapOf(
                "behovId" to behovId,
                "journalpostId" to journalpostId,
            ),
        ) {
            try {
                if (listOf("598125943", "598125958").contains(journalpostId) && System.getenv()["NAIS_CLUSTER_NAME"] == "dev-gcp") {
                    logger.warn { "Skipper journalpost" }
                    return@withMDC
                }

                runBlocking(MDCContext()) {
                    gosysOppslag.opprettOppgave(
                        packet.gosysOppgave(),
                    )
                }.also {
                    packet["@løsning"] = mapOf(
                        BEHOV to mapOf(
                            "journalpostId" to journalpostId,
                            "oppgaveId" to it,
                        ),
                    )
                    context.publish(packet.toJson())
                    logger.info { "Løste behov $BEHOV med løsning $it" }
                }
            } catch (e: Exception) {
                logger.info { "Kunne ikke opprette gosys oppgave for journalpost med id $journalpostId" }
                throw e
            }
        }
    }
}

private fun JsonMessage.gosysOppgave(): GosysOppgaveRequest = GosysOppgaveRequest(
    journalpostId = this["journalpostId"].asText(),
    aktoerId = this["aktørId"].textValue(),
    tildeltEnhetsnr = this["behandlendeEnhetId"].asText(),
    aktivDato = LocalDate.now(),
    beskrivelse = this["oppgavebeskrivelse"].asText(),
)
