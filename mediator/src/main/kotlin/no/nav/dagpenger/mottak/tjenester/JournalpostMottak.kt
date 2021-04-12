package no.nav.dagpenger.mottak.tjenester

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDateTime

private val logg = KotlinLogging.logger {}
internal class JournalpostMottak(
    private val innsendingMediator: InnsendingMediator,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    init {
        River(rapidsConnection).apply {
            validate { it.requireValue("@event_name", "behov") }
            validate { it.require("@opprettet", JsonNode::asLocalDateTime) }
            validate { it.requireKey("@løsning.${Journalpost.name}") }
            validate { it.requireKey("journalpostId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        val journalpostData = packet["@løsning.${Journalpost.name}"].let {
            JournalpostData(
                aktivitetslogg = Aktivitetslogg(),
                journalpostId = packet["journalpostId"].asText(),
                journalpostStatus = "TODO",
                bruker = it["bruker"]?.let { jsonBruker ->
                    JournalpostData.Bruker(
                        id = jsonBruker["id"].asText(),
                        type = JournalpostData.BrukerType.valueOf(jsonBruker["type"].asText())
                    )
                },
                dokumenter = it["dokumenter"].map { jsonDokument ->
                    JournalpostData.DokumentInfo(
                        tittelHvisTilgjengelig = jsonDokument["tittel"].textValue(),
                        dokumentInfoId = jsonDokument["dokumentInfoId"].asText(),
                        brevkode = jsonDokument["brevkode"].asText(),
                    )
                },
                relevanteDatoer = it["relevanteDatoer"].map { jsonDato ->
                    JournalpostData.RelevantDato(
                        dato = jsonDato["dato"].asText(),
                        datotype = JournalpostData.Datotype.valueOf(jsonDato["datotype"].asText())
                    )
                },
                behandlingstema = it["behandlingstema"].textValue()

            )
        }

        innsendingMediator.håndter(journalpostData)
    }

    override fun onError(problems: MessageProblems, context: MessageContext) {
        logg.info { problems }
    }
}
