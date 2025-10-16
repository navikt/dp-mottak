package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.serder.asUUID

private val logger = KotlinLogging.logger { }

internal class DagpengerOppgaveBehovLøser(
    private val saksbehandlingKlient: SaksbehandlingKlient,
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
                validate {
                    it.requireKey(
                        "@behovId",
                        "journalpostId",
                        "fødselsnummer",
                        "fagsakId",
                        "registrertDato",
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
        val registrertTidspunkt = packet["registrertDato"].asLocalDateTime()
        val fagsakId = packet["fagsakId"].asUUID()
        val fødselsnummer = packet["fødselsnummer"].asText()
        withLoggingContext("journalpostId" to "$journalpostId", "fagsakId" to "$fagsakId") {
            runBlocking {
                logger.info { "Skal løse behov $behovNavn " }
                val oppgaveId =
                    saksbehandlingKlient.opprettOppgave(
                        fagsakId = fagsakId,
                        journalpostId = journalpostId,
                        opprettetTidspunkt = registrertTidspunkt,
                        ident = fødselsnummer,
                    )
                val løsning =
                    mapOf(
                        "fagsakId" to fagsakId,
                        "oppgaveId" to oppgaveId,
                    )

                packet["@løsning"] =
                    mapOf(
                        behovNavn to løsning,
                    ).also {
                        logger.info { "Løste behov $behovNavn med løsning $it" }
                    }
                context.publish(key = fødselsnummer, message = packet.toJson())
            }
        }
    }
}
