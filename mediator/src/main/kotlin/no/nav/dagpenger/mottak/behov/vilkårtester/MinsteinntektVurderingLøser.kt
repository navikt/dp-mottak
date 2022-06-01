package no.nav.dagpenger.mottak.behov.vilkårtester

import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC
import kotlin.concurrent.fixedRateTimer

internal class MinsteinntektVurderingLøser(
    oppryddningPeriode: Long = FEM_MINUTTER,
    regelApiClient: RegelApiClient,
    private val repository: MinsteinntektVurderingRepository,
    private val rapidsConnection: RapidsConnection
) {
    private companion object {
        private const val FEM_MINUTTER = 300000.toLong()
        val logger = KotlinLogging.logger { }
    }

    private val minsteInntektVurderingVaktmester = MinsteInntektVurderingVaktmester()

    init {
        StartBehovPacketListener(regelApiClient, rapidsConnection)
        LøsningPacketListener(rapidsConnection)
        fixedRateTimer(
            name = "MinsteinntektVurderingVaktmester",
            daemon = true,
            initialDelay = oppryddningPeriode,
            period = oppryddningPeriode
        ) { minsteInntektVurderingVaktmester.rydd() }
    }

    private inner class StartBehovPacketListener(
        private val regelApiClient: RegelApiClient,
        rapidsConnection: RapidsConnection
    ) :
        River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate { it.demandValue("@event_name", "behov") }
                validate { it.demandAllOrAny("@behov", listOf("MinsteinntektVurdering")) }
                validate { it.rejectKey("@løsning") }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate { it.requireKey("aktørId") }
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

                runBlocking {
                    try {
                        logger.info { "Forsøker å opprette minsteinntektvurderingsbehov i regel-api for journalpost med $journalpostId" }
                        regelApiClient.startMinsteinntektVurdering(
                            aktørId = packet["aktørId"].asText(),
                            journalpostId = journalpostId
                        )
                        repository.lagre(journalpostId, packet)
                    } catch (e: Exception) {
                        logger.warn(e) { "Feil ved start av minsteinntekts vurdering for journalpost med id $journalpostId" }
                        packet["@løsning"] = ikkeFåttSvar()
                        context.publish(packet.toJson())
                    }
                }
            }
        }
    }

    private inner class MinsteInntektVurderingVaktmester {
        fun rydd() {
            logger.info { "Starter MinsteInntektVurderingVaktmester jobb" }

            repository.slettUtgåtteVurderinger().forEach { (jpId, packet) ->
                packet["@løsning"] = ikkeFåttSvar()
                rapidsConnection.publish(jpId, packet.toJson()).also {
                    logger.info { "Ryddet opp utgått innsending for journalpostId $jpId" }
                }
            }
        }
    }

    private inner class LøsningPacketListener(
        rapidsConnection: RapidsConnection
    ) :
        River.PacketListener {
        init {
            River(rapidsConnection).apply {
                validate {
                    it.demandValue("kontekstType", "soknad")
                    it.requireKey("kontekstId")
                    it.requireKey("minsteinntektResultat")
                }
            }.register(this)
        }

        override fun onPacket(packet: JsonMessage, context: MessageContext) {
            val key = repository.fjern(packet["kontekstId"].asText())
            key?.let {
                it["@løsning"] =
                    mapOf("MinsteinntektVurdering" to MinsteinntektVurdering(packet["minsteinntektResultat"]["oppfyllerMinsteinntekt"].asBoolean()))
                context.publish(it.toJson())
                logger.info { "Løste behov for minsteinntekt ${packet["kontekstId"].asText()}" }
            }
        }
    }

    private data class MinsteinntektVurdering(val oppfyllerMinsteArbeidsinntekt: Boolean)

    private fun ikkeFåttSvar() = mapOf(
        "MinsteinntektVurdering" to mapOf(
            "oppfyllerMinsteArbeidsinntekt" to null
        )
    )
}

interface MinsteinntektVurderingRepository {
    fun lagre(journalpostId: String, packet: JsonMessage): Int
    fun fjern(journalpostId: String): JsonMessage?
    fun slettUtgåtteVurderinger(): List<Pair<String, JsonMessage>>
}
