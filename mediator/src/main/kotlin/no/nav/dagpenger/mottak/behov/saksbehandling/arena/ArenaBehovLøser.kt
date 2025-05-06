package no.nav.dagpenger.mottak.behov.saksbehandling.arena

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.withMDC
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging

internal class ArenaBehovLøser(
    arenaOppslag: ArenaOppslag,
    rapidsConnection: RapidsConnection,
) {
    init {
        OpprettArenaOppgaveBehovLøser(arenaOppslag, rapidsConnection)
    }

    private class OpprettArenaOppgaveBehovLøser(
        private val arenaOppslag: ArenaOppslag,
        rapidsConnection: RapidsConnection,
    ) : River.PacketListener {
        companion object {
            private val logger = KotlinLogging.logger { }
        }

        init {
            River(rapidsConnection)
                .apply {
                    precondition { it.requireValue("@event_name", "behov") }
                    precondition {
                        it.requireAllOrAny(
                            "@behov",
                            listOf("OpprettStartVedtakOppgave", "OpprettVurderhenvendelseOppgave"),
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

            if (emptyList<String>().contains(journalpostId)) {
                logger.warn { "SKipper $journalpostId" }
                return
            }

            withMDC(
                mapOf(
                    "behovId" to behovId,
                    "journalpostId" to journalpostId,
                ),
            ) {
                try {
                    runBlocking(MDCContext()) {
                        val behovNavn = packet["@behov"].first().asText()

                        val oppgaveResponse =
                            when (behovNavn) {
                                "OpprettVurderhenvendelseOppgave" ->
                                    arenaOppslag.opprettVurderHenvendelsOppgave(
                                        journalpostId,
                                        packet.arenaOppgaveParametre(),
                                    )

                                "OpprettStartVedtakOppgave" ->
                                    arenaOppslag.opprettStartVedtakOppgave(
                                        journalpostId,
                                        packet.arenaOppgaveParametre(),
                                    )

                                else -> throw IllegalArgumentException("Uventet behov: $behovNavn")
                            }

                        if (oppgaveResponse != null) {
                            packet["@løsning"] =
                                mapOf(
                                    behovNavn to
                                        mapOf(
                                            "journalpostId" to journalpostId,
                                            "fagsakId" to oppgaveResponse.fagsakId,
                                            "oppgaveId" to oppgaveResponse.oppgaveId,
                                        ),
                                ).also {
                                    logger.info { "Løste behov $behovNavn med løsning $it" }
                                }
                        } else {
                            packet["@løsning"] =
                                mapOf(
                                    behovNavn to mapOf("@feil" to "Kunne ikke opprettet Arena oppgave"),
                                ).also {
                                    logger.info { "Løste behov $behovNavn med feil $it" }
                                }
                        }

                        context.publish(packet.toJson())
                    }
                } catch (e: Exception) {
                    logger.error(e) { "Kunne ikke opprette arena sak med journalpostId $journalpostId" }
                    throw e
                }
            }
        }
    }
}

internal fun JsonMessage.arenaOppgaveParametre(): OpprettArenaOppgaveParametere =
    OpprettArenaOppgaveParametere(
        naturligIdent = this["fødselsnummer"].asText(),
        behandlendeEnhetId = this["behandlendeEnhetId"].asText(),
        tilleggsinformasjon = this["tilleggsinformasjon"].asText(),
        registrertDato = this["registrertDato"].asLocalDateTime().toLocalDate(),
        oppgavebeskrivelse = this["oppgavebeskrivelse"].asText(),
    )
