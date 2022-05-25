package no.nav.dagpenger.mottak

import java.time.LocalDate

/*
interface SøknadFakta {
    fun getFakta(faktaNavn: String): List<JsonNode>
    fun getBooleanFaktum(faktaNavn: String): Boolean
    fun getBooleanFaktum(faktaNavn: String, defaultValue: Boolean): Boolean
    fun getChildFakta(faktumId: Int): List<JsonNode>
    fun getField(navn: String): JsonNode

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

private fun SøknadFakta.rettighetstypeUtregning(): List<Map<String, Boolean>> =
    rettighetstypeUtregning(this.avsluttetArbeidsforhold())

private fun SøknadFakta.kanJobbeDeltid() = getBooleanFaktum("reellarbeidssoker.villigdeltid")
private fun SøknadFakta.kanJobbeHvorSomHelst() = getBooleanFaktum("reellarbeidssoker.villigpendle")
private fun SøknadFakta.helseTilAlleTyperJobb() = getBooleanFaktum("reellarbeidssoker.villighelse")
private fun SøknadFakta.villigTilÅBytteYrke() = getBooleanFaktum("reellarbeidssoker.villigjobb")


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
*/
interface SøknadFaktum {
    fun avsluttetArbeidsforholdFraKonkurs(): Boolean
    fun permittertFraFiskeForedling(): Boolean
    fun permittert(): Boolean
    fun eøsBostedsland(): Boolean
    fun eøsArbeidsforhold(): Boolean
    fun avtjentVerneplikt(): Boolean
    fun helseTilAlleTyperJobb(): Boolean
    fun kanJobbeHvorSomHelst(): Boolean
    fun kanJobbeDeltid(): Boolean
    fun villigTilÅBytteYrke(): Boolean
    fun rettighetstypeUtregning(): List<Map<String, Boolean>>
    fun harJobbetIeøsOmråde(): Boolean
    fun fangstOgFisk(): Boolean
    fun verneplikt(): Boolean
    fun ønskerDagpengerFraDato(): LocalDate
    fun søknadstidspunkt(): LocalDate
    fun sisteDagMedLønnEllerArbeidsplikt(): LocalDate
    fun sisteDagMedLønnKonkurs(): LocalDate
    fun sisteDagMedLønnEllerArbeidspliktResten(): LocalDate
    fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold
    fun jobbetUtenforNorge(): Boolean
    fun søknadsId(): String?
}

internal typealias AvsluttedeArbeidsforhold = List<AvsluttetArbeidsforhold>

data class AvsluttetArbeidsforhold(
    val sluttårsak: Sluttårsak,
    val fiskeforedling: Boolean,
    val land: String
) {
    enum class Sluttårsak {
        AVSKJEDIGET,
        ARBEIDSGIVER_KONKURS,
        KONTRAKT_UTGAATT,
        PERMITTERT,
        REDUSERT_ARBEIDSTID,
        SAGT_OPP_AV_ARBEIDSGIVER,
        SAGT_OPP_SELV,
        IKKE_ENDRET
    }
}
