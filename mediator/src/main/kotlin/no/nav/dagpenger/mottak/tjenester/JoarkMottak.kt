package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.observers.Metrics

internal class JoarkMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private companion object {
        private val logg = KotlinLogging.logger {}

        private val forbudteMottaksKanaler =
            setOf(
                "EESSI",
                "NAV_NO_CHAT",
            )
    }

    init {
        River(rapidsConnection)
            .apply {
                validate { it.requireKey("journalpostId") }
                validate { it.requireKey("journalpostStatus") }
                validate { it.requireValue("temaNytt", "DAG") }
                validate { it.requireAny("hendelsesType", listOf("MidlertidigJournalført", "JournalpostMottatt")) }
                validate {
                    it.require("mottaksKanal") { mottaksKanal ->
                        val kanal = mottaksKanal.asText()
                        if (kanal in forbudteMottaksKanaler) throw IllegalArgumentException("Kan ikke håndtere '$kanal' mottakskanal")
                    }
                }
                validate { it.interestedIn("temaNytt", "hendelsesType", "mottaksKanal", "behandlingstema") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        logg.info(
            """Received journalpost with journalpost id: ${packet["journalpostId"].asText()} 
              |tema: ${packet["temaNytt"].asText()}, 
              |hendelsesType: ${packet["hendelsesType"].asText()}, 
              |mottakskanal, ${packet["mottaksKanal"].asText()}, 
              |behandlingstema: ${packet["behandlingstema"].asText()}
              |journalpostStatus: ${packet["journalpostStatus"].asText()}
              |
            """.trimMargin(),
        )

        Metrics.mottakskanalInc(packet["mottaksKanal"].asText())

        val joarkHendelse =
            JoarkHendelse(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = packet["journalpostId"].asText(),
                hendelseType = packet["hendelsesType"].asText(),
                journalpostStatus = packet["journalpostStatus"].asText(),
                behandlingstema = packet["behandlingstema"].asText() ?: null,
                mottakskanal = packet["mottaksKanal"].asText(),
            )

        innsendingMediator.håndter(joarkHendelse)
    }
}
