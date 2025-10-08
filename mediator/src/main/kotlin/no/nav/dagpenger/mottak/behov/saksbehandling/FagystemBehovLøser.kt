package no.nav.dagpenger.mottak.behov.saksbehandling

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Fagsystem
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettOppgave
import no.nav.dagpenger.mottak.KategorisertJournalpost
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting
import java.util.UUID

private val logger = KotlinLogging.logger { }

internal class FagystemBehovLøser(
    private val oppgaveRuting: OppgaveRuting,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        val behovNavn: String = OpprettOppgave.name
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition {
                    it.requireAllOrAny(
                        "@behov",
                        listOf(Fagsystem.name),
                    )
                    it.forbid("@løsning")
                    it.forbid("@feil")
                }
                validate {
                    it.requireKey(
                        "@behovId",
                        "journalpostId",
                        "fødselsnummer",
                        "kategori",
                    )
                    it.interestedIn("søknadsId")
                }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val ident = packet["fødselsnummer"].asText()
        withLoggingContext("journalpostId" to journalpostId) {
            runBlocking {
                val kategori = KategorisertJournalpost.Kategori.valueOf(packet["kategori"].asText())
                val løsning: Map<String, Any> =
                    when (kategori) {
                        KategorisertJournalpost.Kategori.KLAGE -> {
                            lagLøsning(oppgaveRuting.ruteOppgave(ident))
                        }

                        KategorisertJournalpost.Kategori.ETTERSENDING -> {
                            lagLøsning(oppgaveRuting.ruteOppgave(ident, packet["soknadsId"].asUUID()))
                        }

                        else -> {
                            logger.error { "Behov for bestemmelse av fagsystem for kategori $kategori er ikke støttet" }
                            mapOf(
                                "@feil" to "Kunne ikke bestemme fagsystem for journalpostId: $journalpostId med kategori $kategori",
                            )
                        }
                    }
                packet["@løsning"] =
                    mapOf(
                        behovNavn to løsning,
                        "@final" to true,
                    )
            }
            context.publish(packet.toJson())
        }
    }

    private fun lagLøsning(fagsystem: OppgaveRuting.System): Map<String, Any> {
        return when (fagsystem) {
            is OppgaveRuting.System.Dagpenger -> {
                mapOf(
                    "fagsakId" to fagsystem.sakId,
                    "fagsystem" to fagsystem.fagsystem.name,
                )
            }

            OppgaveRuting.System.Arena -> {
                mapOf(
                    "fagsystem" to fagsystem.fagsystem.name,
                )
            }
        }.also {
            logger.info { "Løste behov $behovNavn med løsning $it" }
        }
    }

    private fun JsonNode.asUUID(): UUID = this.asText().let { UUID.fromString(it) }
}
