package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate

internal class OpprettGosysOppgaveLøser(private val gosysOppslag: GosysOppslag, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    private companion object {
        val logger = KotlinLogging.logger { }
    }

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAllOrAny("@behov", listOf("OpprettGosysoppgave"))
                it.rejectKey("@løsning")
                it.requireKey("@id", "journalpostId", "behandlendeEnhetId", "registrertDato")
                it.interestedIn("aktørId", "tilleggsinformasjon", "oppgavebeskrivelse")
            }
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

private fun JsonMessage.gosysOppgave(): GosysOppgaveParametre = GosysOppgaveParametre(
    journalpostId = this["journalpostId"].asText(),
    aktørId = this["aktørId"].asText(),
    tildeltEnhetsnr = this["behandlendeEnhetId"].asText(),
    aktivDato = this["registrertDato"].asLocalDate(),
    beskrivelse = this["oppgavebeskrivelse"].asText()
)
