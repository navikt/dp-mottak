package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import kotlinx.coroutines.runBlocking
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate

internal class OpprettGosysOppgaveLøser(private val gosysOppslag: GosysOppslag, rapidsConnection: RapidsConnection) :
    River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate {
                it.demandValue("@event_name", "behov")
                it.demandAllOrAny("@behov", listOf("OpprettGosysoppgave"))
                it.rejectKey("@løsning")
                it.requireKey("@id", "journalpostId")
            }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            runBlocking {
                gosysOppslag.opprettOppgave(
                    packet.gosysOppgave()
                )
            }

        } catch (e: Exception) {

        }
    }
}

private fun JsonMessage.gosysOppgave(): GosysOppgave = GosysOppgave(
    journalpostId = this["journalpostId"].asText(),
    aktørId = this["aktørid"].asText(),
    tildeltEnhetsnr = this["tildeltEnhetsnr"].asText(),
    beskrivelse = this["beskrivelse"].asText(),
    aktivDato = this["aktivDato"].asLocalDate(), //TODO: whats this?
    fristFerdigstillelse = LocalDate.now().plusWeeks(3) //TODO: er fristen 3 uker?
)
