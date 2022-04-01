package no.nav.dagpenger.mottak.behov.eksterne

import com.fasterxml.jackson.databind.JsonNode
import de.slub.urn.URN
import mu.KotlinLogging
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.SøknadFakta
import no.nav.dagpenger.mottak.avsluttetArbeidsforhold
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River
import no.nav.helse.rapids_rivers.asLocalDate
import no.nav.helse.rapids_rivers.asLocalDateTime
import no.nav.helse.rapids_rivers.withMDC
import java.time.LocalDate
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
                        "Lærling" -> søknad.lærling()
                        "Rettighetstype" -> søknad.rettighetstypeUtregning()
                        "KanJobbeDeltid" -> søknad.kanJobbeDeltid()
                        "KanJobbeHvorSomHelst" -> søknad.kanJobbeHvorSomHelst()
                        "HelseTilAlleTyperJobb" -> søknad.helseTilAlleTyperJobb()
                        "VilligTilÅBytteYrke" -> søknad.villigTilÅBytteYrke()
                        "FortsattRettKorona" -> søknad.fortsattRettKorona()
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

private fun SøknadFakta.sisteDagMedLønnEllerArbeidsplikt(): LocalDate {
    if (getFakta("arbeidsforhold").isEmpty())
        return getFakta("arbeidsforhold.datodagpenger").first()["value"].asLocalDate()
    return when (avsluttetArbeidsforhold().first().sluttårsak) {
        AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS -> sisteDagMedLønnKonkurs()
        else -> sisteDagMedLønnEllerArbeidspliktResten()
    }
}

private fun SøknadFakta.sisteDagMedLønnKonkurs(): LocalDate {
    return getFakta("arbeidsforhold").first().let {
        localDateEllerNull(it["properties"]["lonnkonkursmaaned_dato"])
            ?: it["properties"]["konkursdato"].asLocalDate()
    }
}

// Varighet på arbeidsforholdet (til dato) ?: Lønnspliktperiode for arbeidsgiver (til dato) ?: Arbeidstid redusert fra ?: Permitteringsperiode (fra dato)
private fun SøknadFakta.sisteDagMedLønnEllerArbeidspliktResten(): LocalDate {
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

private fun SøknadFakta.søknadstidspunkt() =
    getFakta("innsendtDato").first()["value"].asLocalDateTime().toLocalDate()

private fun SøknadFakta.ønskerDagpengerFraDato() =
    getFakta("arbeidsforhold.datodagpenger").first()["value"].asLocalDate()

private fun SøknadFakta.verneplikt() = getBooleanFaktum("ikkeavtjentverneplikt", true).not()

// omvendt logikk i søknad; verdi == true --> søker har ikke inntekt fra fangst og fisk
private fun SøknadFakta.fangstOgFisk() = getBooleanFaktum("egennaering.fangstogfiske").not()

// omvendt logikk i søknad; verdi == true --> søker har ikke jobbet i EØS området
private fun SøknadFakta.harJobbetIeøsOmråde() = getBooleanFaktum("eosarbeidsforhold.jobbetieos", true).not()

private fun SøknadFakta.jobbetUtenforNorge() = this.avsluttetArbeidsforhold().any { it.land != "NOR" }

private fun SøknadFakta.fortsattRettKorona() = false

private fun SøknadFakta.rettighetstypeUtregning(): List<Map<String, Boolean>> =
    rettighetstypeUtregning(this.avsluttetArbeidsforhold())

private fun SøknadFakta.kanJobbeDeltid() = getBooleanFaktum("reellarbeidssoker.villigdeltid")
private fun SøknadFakta.kanJobbeHvorSomHelst() = getBooleanFaktum("reellarbeidssoker.villigpendle")
private fun SøknadFakta.helseTilAlleTyperJobb() = getBooleanFaktum("reellarbeidssoker.villighelse")
private fun SøknadFakta.villigTilÅBytteYrke() = getBooleanFaktum("reellarbeidssoker.villigjobb")

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
