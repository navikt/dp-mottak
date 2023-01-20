package no.nav.dagpenger.mottak.behov.eksterne

import de.slub.urn.URN
import mu.KotlinLogging
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.withMDC
import java.time.format.DateTimeParseException

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
        "Verneplikt",
        "FangstOgFiske",
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
            validate { it.interestedIn("søknad_uuid", "@behovId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {

        withMDC(
            mapOf(
                "søknad_uuid" to packet["søknad_uuid"].asText(),
                "behovId" to packet["@behovId"].asText()
            )
        ) {
            try {
                val innsendtSøknadsId = packet.getInnsendtSøknadsId()
                val søknad = søknadQuizOppslag.hentSøknad(innsendtSøknadsId)
                val løsning: Map<String, Any> = packet["@behov"].map { it.asText() }.filter { it in løserBehov }.associateWith { behov ->
                    val avsluttedeArbeidsforhold = søknad.avsluttetArbeidsforhold()
                    val reellArbeidsSøker = søknad.reellArbeidsSøker()
                    when (behov) {
                        "ØnskerDagpengerFraDato" ->
                            søknad.ønskerDagpengerFraDato()
                        "Verneplikt" -> søknad.avtjentVerneplikt()
                        "FangstOgFiske" -> søknad.fangstOgFisk()
                        "EØSArbeid" -> søknad.eøsArbeidsforhold()
                        "Rettighetstype" -> rettighetstypeUtregning(avsluttedeArbeidsforhold)
                        "KanJobbeDeltid" -> reellArbeidsSøker.deltid
                        "KanJobbeHvorSomHelst" -> reellArbeidsSøker.geografi
                        "HelseTilAlleTyperJobb" -> reellArbeidsSøker.helse
                        "VilligTilÅBytteYrke" -> reellArbeidsSøker.yrke
                        "JobbetUtenforNorge" -> jobbetUtenforNorge(avsluttedeArbeidsforhold)
                        else -> throw IllegalArgumentException("Ukjent behov $behov")
                    }
                }
                packet["@løsning"] = løsning
                context.publish(packet.toJson())
                logger.info("løste ${løsning.keys} behov for innsendt søknad med id $innsendtSøknadsId")
            } catch (e: DateTimeParseException) {
                logger.info(e) { "feil ved parsing av dato i søknadfakta-behov. Hopper over behovet" }
                sikkerlogg.info(e) { "feil ved parsing av dato i søknadfakta-behov. Hopper over behovet \n packet: ${packet.toJson()}" }
            } catch (e: Exception) {
                logger.error(e) { "feil ved søknadfakta-behov" }
                sikkerlogg.error(e) { "feil ved søknadfakta-behov. \n packet: ${packet.toJson()}" }
                throw e
            }
        }
    }
}

internal fun jobbetUtenforNorge(avsluttedeArbeidsforhold: List<AvsluttetArbeidsforhold>): Boolean =
    avsluttedeArbeidsforhold.any { it.land != "NOR" }

internal fun rettighetstypeUtregning(avsluttedeArbeidsforhold: List<AvsluttetArbeidsforhold>): List<Map<String, Boolean>> =
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

private fun JsonMessage.getInnsendtSøknadsId(): String {
    return this["InnsendtSøknadsId"]["urn"]
        .asText()
        .let { URN.rfc8141().parse(it) }
        .namespaceSpecificString()
        .toString()
}
