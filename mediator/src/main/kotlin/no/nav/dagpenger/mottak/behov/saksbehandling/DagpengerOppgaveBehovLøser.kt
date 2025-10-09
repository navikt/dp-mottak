package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaOppslag
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.arenaOppgaveParametre
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting

private val logger = KotlinLogging.logger { }

internal class DagpengerOppgaveBehovLøser(
    private val arenaOppslag: ArenaOppslag,
    private val saksbehandlingKlient: SaksbehandlingKlient,
    private val oppgaveRuting: OppgaveRuting,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    companion object {
        val behovNavn: String = Behovtype.OpprettDagpengerOppgave.name
    }

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition {
                    it.requireAllOrAny(
                        "@behov",
                        listOf(behovNavn),
                    )
                }
                precondition { it.forbid("@løsning") }
                precondition { it.forbid("@feil") }
                validate { it.requireKey("@behovId", "journalpostId", "fødselsnummer", "fagsakId", "registrertDato") }
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
        val registrertTidspunkt = packet["registrertDato"].asLocalDateTime()
        runBlocking {
            oppgaveRuting.ruteOppgave(ident).let { system ->
                logger.info { "Skal løse behov $behovNavn i fagsystem $system" }
                when (system) {
                    is Fagsystem.Dagpenger -> {
                        val oppgaveId =
                            saksbehandlingKlient.opprettOppgave(
                                fagsakId = system.sakId,
                                journalpostId = journalpostId,
                                opprettetTidspunkt = registrertTidspunkt,
                                ident = packet["fødselsnummer"].asText(),
                                skjemaKategori = packet["skjemaKategori"].asText(),
                            )
                        val løsning =
                            mapOf(
                                "fagsakId" to system.sakId,
                                "oppgaveId" to oppgaveId,
                                "fagsystem" to system.fagsystemType.name,
                            )

                        packet["@løsning"] =
                            mapOf(
                                behovNavn to løsning,
                            ).also {
                                logger.info { "Løste behov $behovNavn med løsning $it" }
                            }
                        context.publish(packet.toJson())
                    }

                    Fagsystem.Arena -> {
                        val oppgaveResponse =
                            arenaOppslag.opprettVurderHenvendelsOppgave(
                                journalpostId,
                                packet.arenaOppgaveParametre(),
                            )
                        if (oppgaveResponse != null) {
                            packet["@løsning"] =
                                mapOf(
                                    behovNavn to
                                        mapOf(
                                            "journalpostId" to journalpostId,
                                            "fagsakId" to oppgaveResponse.fagsakId,
                                            "oppgaveId" to oppgaveResponse.oppgaveId,
                                            "fagsystem" to system.fagsystemType.name,
                                        ),
                                ).also {
                                    logger.info { "Løste behov $behovNavn med løsning $it" }
                                }
                        } else {
                            packet["@løsning"] =
                                mapOf(
                                    behovNavn to
                                        mapOf(
                                            "@feil" to "Kunne ikke opprette Arena oppgave",
                                            "fagsystem" to system.fagsystemType.name,
                                        ),
                                ).also {
                                    logger.info { "Løste behov $behovNavn med feil $it" }
                                }
                        }
                        context.publish(packet.toJson())
                    }
                }
            }
        }
    }
}
