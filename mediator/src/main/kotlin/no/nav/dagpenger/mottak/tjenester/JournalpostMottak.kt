package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import mu.KotlinLogging
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
                validate { it.requireValue("@event_name", "behov") }
                validate { it.requireValue("@final", true) }
                validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
                validate { it.requireKey(løsning) }
                validate { it.requireKey("journalpostId") }
            }.register(this)
    }

    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        logg.info { "Fått løsning for $løsning, journalpostId=$journalpostId" }
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
                    ).also {
                        logg.info {
                            """Mottok ny journalpost. 
                            |Antall dokumenter=${it.dokumenter().size}, 
                            |brevkode=${it.hovedDokument().brevkode}, 
                            |registrertDato=${it.datoRegistrert()}, 
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
