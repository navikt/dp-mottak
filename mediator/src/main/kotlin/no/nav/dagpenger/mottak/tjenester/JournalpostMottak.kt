package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.micrometer.core.instrument.MeterRegistry
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.JsonMessageExtensions.getOrNull
import no.nav.dagpenger.mottak.meldinger.Journalpost
import tools.jackson.databind.JsonNode
import java.time.LocalDateTime
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype as Behov

private val logg = KotlinLogging.logger {}
private val sikkerLogg = KotlinLogging.logger("tjenestekall.JournalpostMottak")

internal class JournalpostMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    private val løsning = "@løsning.${Behov.Journalpost.name}"

    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition { it.requireValue("@final", true) }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
                validate { it.requireKey(løsning) }
                validate { it.requireKey("journalpostId") }
                validate { it.interestedIn("@behovId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asString()
        logg.info { "Fått løsning for $løsning, journalpostId=$journalpostId" }
        withLoggingContext(
            "journalpostId" to journalpostId,
            "behovId" to packet["@behovId"].asString(),
        ) {
            val journalpostData =
                try {
                    packet[løsning].let {
                        Journalpost(
                            aktivitetslogg = Aktivitetslogg(),
                            journalpostId = journalpostId,
                            journalpostStatus = it["journalstatus"].asString(),
                            bruker =
                                it.getOrNull("bruker")?.let { jsonBruker ->
                                    Journalpost.Bruker(
                                        id = jsonBruker["id"].asString(),
                                        type = Journalpost.BrukerType.valueOf(jsonBruker["type"].asString()),
                                    )
                                },
                            dokumenter =
                                it["dokumenter"].values().map { jsonDokument ->
                                    Journalpost.DokumentInfo(
                                        tittelHvisTilgjengelig = jsonDokument["tittel"].stringValue(),
                                        dokumentInfoId = jsonDokument["dokumentInfoId"].asString(),
                                        brevkode = jsonDokument["brevkode"].asString(),
                                        hovedDokument = jsonDokument["hovedDokument"].asBoolean(),
                                    )
                                },
                            registrertDato =
                                it["relevanteDatoer"]
                                    .values().firstOrNull { relevantDato ->
                                        relevantDato["datotype"].asString() == "DATO_REGISTRERT"
                                    }?.get("dato")
                                    ?.asString()
                                    ?.let { relevantDato -> LocalDateTime.parse(relevantDato) }
                                    ?: LocalDateTime.now(),
                            behandlingstema = it["behandlingstema"].stringValue(),
                            journalførendeEnhet = it["journalfoerendeEnhet"]?.asString(),
                        ).also {
                            logg.info {
                                """Mottok ny journalpost. 
                            |Antall dokumenter=${it.dokumenter().size}, 
                            |brevkode=${it.hovedDokument().brevkode}, 
                            |registrertDato=${it.datoRegistrert()}, 
                            |journalførendeEnhet=${it.journalførendeEnhet()},
                            |behandlingstema=${packet[løsning]["behandlingstema"].stringValue()}
                                """.trimMargin()
                            }
                        }
                    }
                } catch (e: Exception) {
                    sikkerLogg.error { "Klarte ikke å lese $journalpostId, ${packet.toJson()}" }
                    throw e
                }

            innsendingMediator.håndter(journalpostData)
        }
    }
}
