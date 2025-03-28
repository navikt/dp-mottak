package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostDokarkiv
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import java.util.UUID

private val logger = KotlinLogging.logger {}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val innsendingMetadataRepository: InnsendingMetadataRepository,
    private val journalpostDokarkiv: JournalpostDokarkiv,
) : River.PacketListener {
    companion object {
        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireValue("@event_name", "vedtak_fattet")
                it.requireValue("fagsystem", "Dagpenger")
                it.requireValue("behandletHendelse.type", "Søknad")
            }
            validate {
                it.requireKey("ident", "behandlingId", "fagsakId", "automatisk")
                it.interestedIn("behandletHendelse")
            }
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
        val søknadId = packet.søknadId()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok vedtak_fattet hendelse" }
            val arenaOppgaver =
                innsendingMetadataRepository.hentArenaOppgaver(
                    søknadId = søknadId,
                    ident = ident,
                )

            val oppgaveIder = arenaOppgaver.map { it.oppgaveId }
            val arenaFagsakId: String = arenaOppgaver.single { it.fagsakId != null }.fagsakId ?: throw RuntimeException("Kunne ikke hente arena fagsakid")
            val dagpengerFagsakId = packet["fagsakId"].asUUID()

            runBlocking {
                arenaOppgaver.forEach { oppgave ->
                    val nyJournalPostId =
                        journalpostDokarkiv.knyttJounalPostTilNySak(
                            journalpostId = oppgave.journalpostId,
                            dagpengerFagsakId = dagpengerFagsakId.toString(),
                            ident = ident,
                        )
                    innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(
                        jounalpostId = nyJournalPostId.toInt(),
                        innsendingId = oppgave.innsendingId,
                        fagsakId = dagpengerFagsakId,
                    )
                }
            }

            val message =
                JsonMessage.newNeed(
                    behov = listOf("slett_arena_oppgaver"),
                    map =
                        mapOf(
                            "behandlingId" to behandlingId,
                            "arenaFagsakId" to arenaFagsakId,
                            "oppgaveIder" to oppgaveIder,
                        ),
                ).toJson()
            context.publish(ident, message)
        }
    }
}

private fun JsonMessage.søknadId(): UUID = this["behandletHendelse"]["id"].asUUID()

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
