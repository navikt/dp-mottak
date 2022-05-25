package no.nav.dagpenger.mottak.behov.eksterne

import de.slub.urn.URN
import mu.KotlinLogging
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC

internal class SøknadFaktaQuizLøser(
    private val søknadQuizOppslag: SøknadQuizOppslag,
    rapidsConnection: RapidsConnection
) : River.PacketListener {

    private companion object {
        val logger = KotlinLogging.logger { }
        val sikkerlogg = KotlinLogging.logger("tjenestekall")
    }

    private val løserBehov = listOf(
        "ØnskerDagpengerFraDato",
        "Søknadstidspunkt",
        "Verneplikt",
        "FangstOgFiske",
        "SisteDagMedArbeidsplikt",
        "SisteDagMedLønn",
        "Lærling",
        "EØSArbeid",
        "Rettighetstype",
        "KanJobbeDeltid",
        "KanJobbeHvorSomHelst",
        "HelseTilAlleTyperJobb",
        "VilligTilÅBytteYrke",
        "FortsattRettKorona",
        "JobbetUtenforNorge"
    )

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "faktum_svar") }
            validate { it.demandAllOrAny("@behov", løserBehov) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("InnsendtSøknadsId") }
            validate { it.interestedIn("søknad_uuid", "@id") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        withMDC(
            mapOf(
                "søknad_uuid" to packet["søknad_uuid"].asText(),
                "behovId" to packet["@id"].asText()
            )
        ) {
            try {
                val innsendtSøknadsId = packet.getInnsendtSøknadsId()
                val søknad = søknadQuizOppslag.hentSøknad(innsendtSøknadsId)
                packet["@løsning"] = packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
                    behov to when (behov) {
                        "ØnskerDagpengerFraDato" ->
                            søknad.ønskerDagpengerFraDato()
                        "Søknadstidspunkt" -> søknad.søknadstidspunkt()
                        "Verneplikt" -> søknad.verneplikt()
                        "FangstOgFiske" -> søknad.fangstOgFisk()
                        "EØSArbeid" -> søknad.harJobbetIeøsOmråde()
                        "SisteDagMedArbeidsplikt" -> søknad.sisteDagMedLønnEllerArbeidsplikt()
                        "SisteDagMedLønn" -> søknad.sisteDagMedLønnEllerArbeidsplikt()
                        "Rettighetstype" -> søknad.rettighetstypeUtregning()
                        "KanJobbeDeltid" -> søknad.kanJobbeDeltid()
                        "KanJobbeHvorSomHelst" -> søknad.kanJobbeHvorSomHelst()
                        "HelseTilAlleTyperJobb" -> søknad.helseTilAlleTyperJobb()
                        "VilligTilÅBytteYrke" -> søknad.villigTilÅBytteYrke()
                        "JobbetUtenforNorge" -> søknad.jobbetUtenforNorge()
                        else -> throw IllegalArgumentException("Ukjent behov $behov")
                    }
                }.toMap()

                context.publish(packet.toJson())
                logger.info("løste søknadfakta-behov for innsendt søknad med id $innsendtSøknadsId")
            } catch (e: Exception) {
                logger.error(e) { "feil ved søknadfakta-behov" }
                sikkerlogg.error(e) { "feil ved søknadfakta-behov. \n packet: ${packet.toJson()}" }
                throw e
            }
        }
    }
}

private fun JsonMessage.getInnsendtSøknadsId(): String {
    return this["InnsendtSøknadsId"]["urn"]
        .asText()
        .let { URN.rfc8141().parse(it) }
        .namespaceSpecificString()
        .toString()
}

internal fun rettighetstypeUtregning(avsluttedeArbeidsforhold: List<AvsluttetArbeidsforhold>) =
    avsluttedeArbeidsforhold.map {
        mapOf(
            "Lønnsgaranti" to (it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS),
            "PermittertFiskeforedling" to (it.fiskeforedling),
            "Permittert" to (it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT && !it.fiskeforedling),
            "Ordinær" to (
                it.sluttårsak != AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT &&
                    it.sluttårsak != AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS &&
                    !it.fiskeforedling
                )
        )
    }
