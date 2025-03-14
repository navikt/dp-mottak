package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val innsendingMetadataRepository: InnsendingMetadataRepository,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireValue("fagsystem", "Dagpenger")
            }
            validate { it.requireKey("ident", "søknadId", "behandlingId", "fagsakId", "automatisk") }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val fagsakId = packet["fagsakId"].asText()
        val ident = packet["ident"].asText()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok vedtak_fattet hendelse" }
            val oppgaverIder =
                innsendingMetadataRepository.hentOppgaverIder(
                    søknadId = søknadId,
                    ident = ident,
                )
            // TODO
//            val message =
//                JsonMessage.newNeed(
//                    behov = listOf("slett_arena_oppgaver"),
//                    map =
//                        mapOf(
//                            "fagsakId" to slettArenaOppgaveParametere.fagsakId,
//                            "oppgaveIder" to slettArenaOppgaveParametere.oppgaveIder,
//                        ),
//                ).toJson().also {
//                    no.nav.dagpenger.mottak.behov.saksbehandling.arena.logger.info { "Publiserer behov: $it" }
//                }
        }
    }
}

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
