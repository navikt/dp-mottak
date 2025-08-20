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
                it.requireValue("@event_name", "vedtak_fattet_utenfor_arena")
            }
            validate {
                it.requireKey("behandlingId", "søknadId", "ident", "sakId")
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
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asText()
        val dagpengerFagsakId = packet["sakId"].asUUID()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            logger.info { "Mottok vedtak_fattet_utenfor_arena" }
            val arenaOppgaver =
                innsendingMetadataRepository.hentArenaOppgaver(
                    søknadId = søknadId,
                    ident = ident,
                )

            val oppgaveIder = arenaOppgaver.map { it.oppgaveId }

            runBlocking {
                arenaOppgaver.forEach { oppgave ->
                    val knyttJounalPostTilNySakResponse =
                        journalpostDokarkiv.knyttJounalPostTilNySak(
                            journalpostId = oppgave.journalpostId,
                            dagpengerFagsakId = dagpengerFagsakId.toString(),
                            ident = ident,
                        )

                    innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(
                        jounalpostId = knyttJounalPostTilNySakResponse.nyJournalpostId,
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
                            "oppgaveIder" to oppgaveIder,
                            "ident" to ident,
                        ),
                ).toJson()
            context.publish(ident, message)
        }
    }
}

fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
