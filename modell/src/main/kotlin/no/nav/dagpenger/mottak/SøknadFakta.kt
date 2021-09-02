package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode

interface SøknadFakta {
    fun getFakta(faktaNavn: String): List<JsonNode>
    fun getBooleanFaktum(faktaNavn: String): Boolean
    fun getBooleanFaktum(faktaNavn: String, defaultValue: Boolean): Boolean
    fun getChildFakta(faktumId: Int): List<JsonNode>
    fun getField(navn: String): JsonNode
}

internal typealias AvsluttedeArbeidsforhold = List<AvsluttetArbeidsforhold>

fun SøknadFakta.avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
    return this.getFakta("arbeidsforhold")
        .map {
            AvsluttetArbeidsforhold(
                sluttårsak = asÅrsak(it["properties"]["type"].asText()),
                fiskeforedling = it["properties"]["fangstogfiske"]?.asBoolean() ?: false,
                land = it["properties"]["land"].asText()
            )
        }
}

fun SøknadFakta.harAvsluttetArbeidsforholdFraKonkurs(): Boolean =
    this.avsluttetArbeidsforhold().any { it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS }

fun SøknadFakta.erPermittertFraFiskeForedling(): Boolean =
    this.avsluttetArbeidsforhold().any { it.fiskeforedling }

fun SøknadFakta.erFornyetRettighet(): Boolean = this.getBooleanFaktum("fornyetrett.onskerutvidelse", true).not()

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

private fun asÅrsak(type: String): AvsluttetArbeidsforhold.Sluttårsak = when (type) {
    "permittert" -> AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT
    "avskjediget" -> AvsluttetArbeidsforhold.Sluttårsak.AVSKJEDIGET
    "kontraktutgaatt" -> AvsluttetArbeidsforhold.Sluttårsak.KONTRAKT_UTGAATT
    "redusertarbeidstid" -> AvsluttetArbeidsforhold.Sluttårsak.REDUSERT_ARBEIDSTID
    "sagtoppavarbeidsgiver" -> AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
    "sagtoppselv" -> AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_SELV
    "arbeidsgivererkonkurs" -> AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS
    "ikke-endret" -> AvsluttetArbeidsforhold.Sluttårsak.IKKE_ENDRET
    else -> throw Exception("Missing permitteringstype: $type")
}

internal fun SøknadFakta.harInntektFraFangstOgFiske(): Boolean =
    this.getBooleanFaktum("egennaering.fangstogfiske", false).not()

internal fun SøknadFakta.harEøsArbeidsforhold(): Boolean =
    this.getBooleanFaktum("eosarbeidsforhold.jobbetieos", true).not()

private val eøsLand = setOf("BEL", "BGR", "DNK", "EST", "FIN", "FRA", "GRC", "IRL", "ISL", "ITA", "HRV", "CYP", "LVA", "LIE", "LTU", "LUX", "MLT", "NLD", "POL", "PRT", "ROU", "SVK", "SVN", "ESP", "GBR", "CHE", "SWE", "CZE", "DEU", "HUN", "AUT")

internal fun SøknadFakta.harEøsBostedsland(): Boolean =
    this.getFakta("bostedsland.land").any { it["value"].asText() in eøsLand }

internal fun SøknadFakta.harAvtjentVerneplikt(): Boolean =
    this.getFakta("ikkeavtjentverneplikt").getOrNull(0)?.get("value")?.asBoolean()?.not() ?: false
