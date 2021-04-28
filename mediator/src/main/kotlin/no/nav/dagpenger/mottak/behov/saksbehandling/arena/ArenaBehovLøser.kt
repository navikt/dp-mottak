package no.nav.dagpenger.mottak.behov.saksbehandling.arena

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
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
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandAllOrAny("@behov", listOf("EksisterendeSaker"))
                    it.rejectKey("@løsning")
                    it.requireKey("@id", "journalpostId")
                    it.requireKey("fnr")
                }
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
                validate {
                    it.demandValue("@event_name", "behov")
                    it.demandAllOrAny("@behov", listOf("OpprettStartVedtakOppgave"))
                    it.rejectKey("@løsning")
                    it.requireKey("@id", "journalpostId")
                    it.requireKey(
                        "fødselsnummer",
                        "behandlendeEnhetId",
                        "oppgavebeskrivelse",
                        "registrertDato",
                        "tilleggsinformasjon",
                        "aktørId"
                    )
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            try {
                runBlocking {
                    arenaOppslag.opprettStartVedtakOppgave(
                        fødselsnummer = packet["fødselsnummer"].asText(),
                        aktørId = packet["aktørId"].asText(),
                        behandlendeEnhet = packet["behandlendeEnhetId"].asText(),
                        beskrivelse = packet["oppgavebeskrivelse"].asText(),
                        tilleggsinformasjon = packet["tilleggsinformasjon"].asText(),
                        registrertDato = packet["registrertDato"].asLocalDateTime(),
                        journalpostId = packet["journalpostId"].asText()
                    ).also {
                        packet["@løsning"] = mapOf("OpprettStartVedtakOppgave" to it)
                        context.publish(packet.toJson())
                    }
                }
            } catch (e: Exception) {
                logger.info { "Kunne ikke hente eksisterende saker for søknad med journalpostId ${packet["journalpostId"]}" }
                throw e
            }
        }
    }
}
