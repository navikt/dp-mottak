package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime
import java.time.LocalDateTime
import java.time.ZoneId
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype as Behov

private val logg = KotlinLogging.logger {}

internal class JournalpostMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    private val løsning = "@løsning.${Behov.Journalpost.name}"

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireKey(løsning) }
            validate { it.requireKey("journalpostId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        logg.info { "Fått løsning for $løsning, journalpostId: ${packet["journalpostId"]}" }
        val journalpostData = packet[løsning].let {
            val oslo = ZoneId.of("Europe/Oslo")
            Journalpost(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = packet["journalpostId"].asText(),
                journalpostStatus = "TODO",
                bruker = it["bruker"]?.let { jsonBruker ->
                    Journalpost.Bruker(
                        id = jsonBruker["id"].asText(),
                        type = Journalpost.BrukerType.valueOf(jsonBruker["type"].asText())
                    )
                },
                dokumenter = it["dokumenter"].map { jsonDokument ->
                    Journalpost.DokumentInfo(
                        tittelHvisTilgjengelig = jsonDokument["tittel"].textValue(),
                        dokumentInfoId = jsonDokument["dokumentInfoId"].asText(),
                        brevkode = jsonDokument["brevkode"].asText()
                    )
                },
                registrertDato = it["relevanteDatoer"].firstOrNull {
                    it["datotype"].asText() == "DATO_REGISTRERT"
                }?.get("dato")?.asText().let { LocalDateTime.parse(it).atZone(oslo) } ?: LocalDateTime.now()
                    .atZone(oslo),
                behandlingstema = it["behandlingstema"].textValue()
            )
        }

        innsendingMediator.håndter(journalpostData)
    }
}
