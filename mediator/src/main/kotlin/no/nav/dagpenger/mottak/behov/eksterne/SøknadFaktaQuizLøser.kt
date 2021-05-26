package no.nav.dagpenger.mottak.behov.eksterne

import com.fasterxml.jackson.databind.JsonNode
import mu.KotlinLogging
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.SøknadFakta
import no.nav.dagpenger.mottak.avsluttetArbeidsforhold
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import java.time.LocalDate
import java.time.ZonedDateTime
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
        "Søknadstidspunkt",
        "Verneplikt",
        "FangstOgFiske",
        "SisteDagMedArbeidsplikt",
        "SisteDagMedLønn",
        "Lærling",
        "EØSArbeid",
        "Rettighetstype"
    )

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "faktum_svar") }
            validate { it.demandAllOrAny("@behov", løserBehov) }
            validate { it.rejectKey("@løsning") }
            validate { it.requireKey("InnsendtSøknadsId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        try {
            val søknad = søknadQuizOppslag.hentSøknad(packet["InnsendtSøknadsId"]["url"].asText())
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
                    "Lærling" -> søknad.lærling()
                    "Rettighetstype" -> søknad.rettighetstypeUtregning()

                    else -> throw IllegalArgumentException("Ukjent behov $behov")
                }
            }.toMap()

            context.publish(packet.toJson())
            logger.info("løste søknadfakta-behov for innsendt søknad med id ${packet["InnsendtSøknadsId"]["url"].asText()}")
        } catch (e: Exception) {
            // midlertig til vi klarer å nøste opp i det som faktisk får dette til å kræsje
            logger.error { e }
            sikkerlogg.error { "feil ved søknadfakta-behov, ${e.message}, packet: ${packet.toJson()}" }
        }
    }
}

// Varighet på arbeidsforholdet (til dato) ?: Lønnspliktperiode for arbeidsgiver (til dato) ?: Arbeidstid redusert fra ?: Permitteringsperiode (fra dato)
private fun SøknadFakta.sisteDagMedLønnEllerArbeidsplikt(): LocalDate {
    if (getFakta("arbeidsforhold").isEmpty())
        return getFakta("arbeidsforhold.datodagpenger").first()["value"].asLocalDate()
    return getFakta("arbeidsforhold").first().let {
        localDateEllerNull(it["properties"]["datotil"])
            ?: localDateEllerNull(it["properties"]["lonnspliktigperiodedatotil"])
            ?: localDateEllerNull(it["properties"]["redusertfra"])
            ?: getFakta("arbeidsforhold.permitteringsperiode")
                .first()["properties"]["permiteringsperiodedatofra"].asLocalDate()
    }
}

private fun localDateEllerNull(jsonNode: JsonNode?): LocalDate? = jsonNode?.let {
    try {
        LocalDate.parse(jsonNode.asText())
    } catch (e: DateTimeParseException) { // Optional datoer får noen ganger verdi “NaN-aN-aN”
        null
    }
}

private fun SøknadFakta.lærling() = getFakta("arbeidsforhold").any {
    it["properties"]?.get("laerling")?.asBoolean() ?: false
}

private fun SøknadFakta.søknadstidspunkt() = ZonedDateTime.parse(getField("sistLagret").asText()).toLocalDate()

private fun SøknadFakta.ønskerDagpengerFraDato() =
    getFakta("arbeidsforhold.datodagpenger").first()["value"].asLocalDate()

private fun SøknadFakta.verneplikt() = getBooleanFaktum("ikkeavtjentverneplikt", true).not()

// omvendt logikk i søknad; verdi == true --> søker har ikke inntekt fra fangst og fisk
private fun SøknadFakta.fangstOgFisk() = getBooleanFaktum("egennaering.fangstogfiske").not()

// omvendt logikk i søknad; verdi == true --> søker har ikke jobbet i EØS området
private fun SøknadFakta.harJobbetIeøsOmråde() = getBooleanFaktum("eosarbeidsforhold.jobbetieos", true).not()

private fun SøknadFakta.rettighetstypeUtregning(): List<Map<String, Boolean>> = rettighetstypeUtregning(this.avsluttetArbeidsforhold())

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
