package no.nav.dagpenger.mottak.behov.eksterne

import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.SøknadFakta
import no.nav.dagpenger.mottak.avsluttetArbeidsforhold
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asOptionalLocalDate
import java.time.ZonedDateTime

internal class SøknadFaktaQuizLøser(
    private val søknadQuizOppslag: SøknadQuizOppslag,
    rapidsConnection: RapidsConnection
) :
    River.PacketListener {
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
            validate { it.requireKey("InnsendtSøknadsId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
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
    }
}

// Varighet på arbeidsforholdet (til dato) ?: Lønnspliktperiode for arbeidsgiver (til dato) ?: Arbeidstid redusert fra ?: Permitteringsperiode (fra dato)
private fun SøknadFakta.sisteDagMedLønnEllerArbeidsplikt() = getFakta("arbeidsforhold").first()
    .let {
        it["properties"]["datotil"]?.asOptionalLocalDate()
            ?: it["properties"]["lonnspliktigperiodedatotil"]?.asOptionalLocalDate()
            ?: it["properties"]["redusertfra"]?.asOptionalLocalDate()
            ?: getFakta("arbeidsforhold.permitteringsperiode")
                .first()["properties"]["permiteringsperiodedatofra"].asLocalDate()
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
