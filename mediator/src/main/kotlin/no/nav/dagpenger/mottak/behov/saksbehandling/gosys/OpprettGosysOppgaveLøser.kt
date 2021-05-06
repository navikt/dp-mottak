package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class OpprettGosysOppgaveLøser(private val gosysOppslag: GosysOppslag, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "behov") }
            validate { it.demandAllOrAny("@behov", listOf("OpprettGosysoppgave")) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("@id", "journalpostId", "behandlendeEnhetId", "registrertDato") }
            validate { it.interestedIn("aktørId", "tilleggsinformasjon", "oppgavebeskrivelse") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            runBlocking {
                gosysOppslag.opprettOppgave(
                    packet.gosysOppgave()
                )
            }.also {

                packet["@løsning"] = mapOf(
                    "OpprettGosysoppgave" to mapOf(
                        "journalpostId" to packet["journalpostId"],
                        "oppgaveId" to it
                    )
                )
                context.publish(packet.toJson())
            }
        } catch (e: Exception) {
            logger.info { "Kunne ikke opprette gosys oppgave for journalpost med id ${packet["journalpostId"]}" }
            throw e
        }
    }
}

private fun JsonMessage.gosysOppgave(): GosysOppgaveRequest = GosysOppgaveRequest(
    journalpostId = this["journalpostId"].asText(),
    aktoerId = this["aktørId"].textValue(),
    tildeltEnhetsnr = this["behandlendeEnhetId"].asText(),
    aktivDato = this["registrertDato"].asLocalDateTime().toLocalDate(),
    beskrivelse = this["oppgavebeskrivelse"].asText()
)
