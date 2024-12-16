package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import mu.KotlinLogging
import mu.withLoggingContext
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.JsonMessageExtensions.getOrNull
import no.nav.dagpenger.mottak.meldinger.Journalpost
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
        val journalpostId = packet["journalpostId"].asText()
        logg.info { "Fått løsning for $løsning, journalpostId=$journalpostId" }
        withLoggingContext(
            "journalpostId" to journalpostId,
            "behovId" to packet["@behovId"].asText(),
        ) {
            val journalpostData =
                try {
                    packet[løsning].let {
                        Journalpost(
                            aktivitetslogg = Aktivitetslogg(),
                            journalpostId = journalpostId,
                            journalpostStatus = it["journalstatus"].asText(),
                            bruker =
                                it.getOrNull("bruker")?.let { jsonBruker ->
                                    Journalpost.Bruker(
                                        id = jsonBruker["id"].asText(),
                                        type = Journalpost.BrukerType.valueOf(jsonBruker["type"].asText()),
                                    )
                                },
                            dokumenter =
                                it["dokumenter"].map { jsonDokument ->
                                    Journalpost.DokumentInfo(
                                        tittelHvisTilgjengelig = jsonDokument["tittel"].textValue(),
                                        dokumentInfoId = jsonDokument["dokumentInfoId"].asText(),
                                        brevkode = jsonDokument["brevkode"].asText(),
                                        hovedDokument = jsonDokument["hovedDokument"].asBoolean(),
                                    )
                                },
                            registrertDato =
                                it["relevanteDatoer"]
                                    .firstOrNull {
                                        it["datotype"].asText() == "DATO_REGISTRERT"
                                    }?.get("dato")
                                    ?.asText()
                                    .let { LocalDateTime.parse(it) } ?: LocalDateTime.now(),
                            behandlingstema = it["behandlingstema"].textValue(),
                            journalførendeEnhet = it["journalfoerendeEnhet"]?.asText(),
                        ).also {
                            logg.info {
                                """Mottok ny journalpost. 
                            |Antall dokumenter=${it.dokumenter().size}, 
                            |brevkode=${it.hovedDokument().brevkode}, 
                            |registrertDato=${it.datoRegistrert()}, 
                            |journalførendeEnhet=${it.journalførendeEnhet()},
                            |behandlingstema=${packet[løsning]["behandlingstema"].textValue()}
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
