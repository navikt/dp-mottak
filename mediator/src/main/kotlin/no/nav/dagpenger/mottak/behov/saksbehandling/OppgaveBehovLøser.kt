package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettOppgave
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaOppslag
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.arenaOppgaveParametre
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.OppgaveRuting
import java.util.UUID

private val logger = KotlinLogging.logger { }

internal class OppgaveBehovLøser(
    private val arenaOppslag: ArenaOppslag,
    private val oppgaveKlient: OppgaveKlient,
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
                        listOf(OpprettOppgave.name),
                    )
                }
                precondition { it.forbid("@løsning") }
                precondition { it.forbid("@feil") }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate {
                    it.requireKey(
                        "fødselsnummer",
                        "behandlendeEnhetId",
                        "oppgavebeskrivelse",
                        "registrertDato",
                        "tilleggsinformasjon",
                        "skjemaKategori",
                    )
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
        val behovId = packet["@behovId"].asText()
        val registrertTidspunkt = packet["registrertDato"].asLocalDateTime()

        // Vi får sak fra et eller annet sted
        val sakId = UUID.randomUUID()
        oppgaveRuting.ruteOppgave().let { system ->
            logger.info { "Skal løse behov $behovNavn i fagsystem $system" }
            when (system) {
                OppgaveRuting.FagSystem.DAGPENGER -> {
                    runBlocking {
                        val oppgaveId =
                            oppgaveKlient.opprettOppgave(
                                fagsakId = sakId,
                                journalpostId = journalpostId,
                                opprettetTidspunkt = registrertTidspunkt,
                                ident = packet["fødselsnummer"].asText(),
                                skjemaKategori = packet["skjemaKategori"].asText(),
                            )
                        val løsning =
                            mapOf(
                                "fagsakId" to sakId,
                                "oppgaveId" to oppgaveId,
                                "fagsystem" to system.name,
                            )

                        packet["@løsning"] =
                            mapOf(
                                behovNavn to løsning,
                            ).also {
                                logger.info { "Løste behov $behovNavn med løsning $it" }
                            }
                        context.publish(packet.toJson())
                    }
                }

                OppgaveRuting.FagSystem.ARENA -> {
                    runBlocking {
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
                                            "fagsystem" to system.name,
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
                                            "fagsystem" to system.name,
                                        ),
                                ).also {
                                    logger.info { "Løste behov $behovNavn med feil $it" }
                                }
                        }
                    }
                    context.publish(packet.toJson())
                }
            }
        }
    }
}
