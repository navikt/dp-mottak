package no.nav.dagpenger.mottak.behov.saksbehandling.arena

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.slf4j.MDCContext
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.withMDC

internal class ArenaBehovLøser(arenaOppslag: ArenaOppslag, rapidsConnection: RapidsConnection) {

    init {
        EksisterendeSakerBehovLøser(arenaOppslag, rapidsConnection)
        OpprettArenaOppgaveBehovLøser(arenaOppslag, rapidsConnection)
    }

    private class EksisterendeSakerBehovLøser(
        private val arenaOppslag: ArenaOppslag,
        rapidsConnection: RapidsConnection
    ) : River.PacketListener {

        companion object {
            private val logger = KotlinLogging.logger { }
        }

        init {
            River(rapidsConnection).apply {
                validate { it.demandValue("@event_name", "behov") }
                validate { it.demandAllOrAny("@behov", listOf("EksisterendeSaker")) }
                validate { it.rejectKey("@løsning") }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate { it.requireKey("fnr") }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val journalpostId = packet["journalpostId"].asText()
            val behovId = packet["@behovId"].asText()

            withMDC(
                mapOf(
                    "behovId" to behovId,
                    "journalpostId" to journalpostId
                )
            ) {
                try {
                    runBlocking(MDCContext()) {
                        arenaOppslag.harEksisterendeSaker(packet["fnr"].asText()).also {
                            packet["@løsning"] = mapOf("EksisterendeSaker" to mapOf("harEksisterendeSak" to it))
                            context.publish(packet.toJson())
                        }
                    }
                } catch (e: Exception) {
                    logger.info { "Kunne ikke hente eksisterende saker for søknad med journalpostId $journalpostId" }
                    throw e
                }
            }
        }
    }

    private class OpprettArenaOppgaveBehovLøser(
        private val arenaOppslag: ArenaOppslag,
        rapidsConnection: RapidsConnection
    ) : River.PacketListener {

        companion object {
            private val logger = KotlinLogging.logger { }
        }

        init {
            River(rapidsConnection).apply {
                validate { it.demandValue("@event_name", "behov") }
                validate {
                    it.demandAllOrAny(
                        "@behov",
                        listOf("OpprettStartVedtakOppgave", "OpprettVurderhenvendelseOppgave")
                    )
                }
                validate { it.rejectKey("@løsning") }
                validate { it.rejectKey("@feil") }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate {
                    it.requireKey(
                        "fødselsnummer",
                        "behandlendeEnhetId",
                        "oppgavebeskrivelse",
                        "registrertDato",
                        "tilleggsinformasjon"
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {

            val journalpostId = packet["journalpostId"].asText()
            val behovId = packet["@behovId"].asText()

            if (emptyList<String>().contains(journalpostId)) {
                logger.warn { "SKipper $journalpostId" }
                return
            }

            withMDC(
                mapOf(
                    "behovId" to behovId,
                    "journalpostId" to journalpostId
                )
            ) {
                try {
                    runBlocking(MDCContext()) {
                        val behovNavn = packet["@behov"].first().asText()

                        val oppgaveResponse = when (behovNavn) {
                            "OpprettVurderhenvendelseOppgave" -> arenaOppslag.opprettVurderHenvendelsOppgave(
                                journalpostId,
                                packet.arenaOppgaveParametre()
                            )

                            "OpprettStartVedtakOppgave" -> arenaOppslag.opprettStartVedtakOppgave(
                                journalpostId,
                                packet.arenaOppgaveParametre()
                            )

                            else -> throw IllegalArgumentException("Uventet behov: $behovNavn")
                        }

                        if (oppgaveResponse != null) {
                            packet["@løsning"] = mapOf(
                                behovNavn to mapOf(
                                    "journalpostId" to journalpostId,
                                    "fagsakId" to oppgaveResponse.fagsakId,
                                    "oppgaveId" to oppgaveResponse.oppgaveId
                                )
                            ).also {
                                logger.info { "Løste behov $behovNavn med løsning $it" }
                            }
                        } else {
                            packet["@løsning"] = mapOf(
                                behovNavn to mapOf("@feil" to "Kunne ikke opprettet Arena oppgave")
                            )
                                .also {
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

private fun JsonMessage.arenaOppgaveParametre(): OpprettArenaOppgaveParametere = OpprettArenaOppgaveParametere(
    naturligIdent = this["fødselsnummer"].asText(),
    behandlendeEnhetId = this["behandlendeEnhetId"].asText(),
    tilleggsinformasjon = this["tilleggsinformasjon"].asText(),
    registrertDato = this["registrertDato"].asLocalDateTime().toLocalDate(),
    oppgavebeskrivelse = this["oppgavebeskrivelse"].asText(),
)
