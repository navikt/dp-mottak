package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaKlient

internal class SlettArenaOppgaverBehovløser(rapid: RapidsConnection, private val arenaKlient: ArenaKlient) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "behov")
                it.requireAll("@behov", listOf("slett_arena_oppgaver"))
                it.requireKey("oppgaveIder", "arenaFagsakId", "ident")
            }
        }
    }

    init {
        River(rapid).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val arenaFagsakId: String = packet.arenaFagSakId()
        val oppgaverSomSkalSlettes: List<String> = packet.oppgaverSomSkalSlettes()

        runBlocking {
            arenaKlient.slettOppgaver(arenaFagsakId, oppgaverSomSkalSlettes)
        }
    }

    private fun JsonMessage.arenaFagSakId(): String = this["arenaFagsakId"].asText()

    private fun JsonMessage.oppgaverSomSkalSlettes(): List<String> = this["oppgaveIder"].map { it.asText() }
}
