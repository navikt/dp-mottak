package no.nav.dagpenger.mottak.behov.saksbehandling.arena

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

internal class ArenaBehovLøser(arenaOppslag: ArenaOppslag, rapidsConnection: RapidsConnection) {

    // todo: OpprettVurderhenvendelseOppgave
    init {
        EksisterendeSakerLøser(arenaOppslag, rapidsConnection)
        OpprettStartVedtakOppgaveLøser(arenaOppslag, rapidsConnection)
    }

    private class EksisterendeSakerLøser(
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
                validate { it.requireKey("@id", "journalpostId") }
                validate { it.requireKey("fnr") }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            try {
                runBlocking {
                    arenaOppslag.harEksisterendeSaker(packet["fnr"].asText()).also {
                        packet["@løsning"] = mapOf("EksisterendeSaker" to mapOf("harEksisterendeSak" to it))
                        context.publish(packet.toJson())
                    }
                }
            } catch (e: Exception) {
                logger.info { "Kunne ikke hente eksisterende saker for søknad med journalpostId ${packet["journalpostId"]}" }
                throw e
            }
        }
    }

    private class OpprettStartVedtakOppgaveLøser(
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
                validate { it.requireKey("@id", "journalpostId") }
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
            try {
                runBlocking {
                    val behovNavn = packet["@behov"].first().asText()
                    val response = when (behovNavn) {
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
                    packet["@løsning"] = mapOf(behovNavn to response)
                    context.publish(packet.toJson())
                }
            } catch (e: Exception) {
                logger.error(e) { "Kunne opprette arena sak med journalpostId ${packet["journalpostId"]}" }
                throw e
            }
        }

        override fun onSevere(error: MessageProblems.MessageException, context: MessageContext) {
            logger.error(error) { error.problems }
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
