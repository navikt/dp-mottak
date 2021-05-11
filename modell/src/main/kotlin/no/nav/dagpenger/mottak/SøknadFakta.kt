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
    return this.getFakta("arbeidsforhold").map {
        AvsluttetArbeidsforhold(
            sluttårsak = asÅrsak(it["properties"]["type"].asText()),
            grensearbeider = !this.getBooleanFaktum("arbeidsforhold.grensearbeider", true),
            fiskeforedling = it["properties"]["fangstogfiske"]?.asBoolean() ?: false
        )
    }
}

fun SøknadFakta.erGrenseArbeider(): Boolean =
    this.avsluttetArbeidsforhold().any { it.grensearbeider }

fun SøknadFakta.harAvsluttetArbeidsforholdFraKonkurs(): Boolean =
    this.avsluttetArbeidsforhold().any { it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS }

fun SøknadFakta.erPermittertFraFiskeForedling(): Boolean =
    this.avsluttetArbeidsforhold().any { it.fiskeforedling }

fun SøknadFakta.erFornyetRettighet(): Boolean = this.getBooleanFaktum("fornyetrett.onskerutvidelse", true).not()

data class AvsluttetArbeidsforhold(
    val sluttårsak: Sluttårsak,
    val grensearbeider: Boolean,
    val fiskeforedling: Boolean
) {
    enum class Sluttårsak {
        AVSKJEDIGET,
        ARBEIDSGIVER_KONKURS,
        KONTRAKT_UTGAATT,
        PERMITTERT,
        REDUSERT_ARBEIDSTID,
        SAGT_OPP_AV_ARBEIDSGIVER,
        SAGT_OPP_SELV
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
    else -> throw Exception("Missing permitteringstype")
}

internal fun SøknadFakta.harInntektFraFangstOgFiske(): Boolean =
    this.getBooleanFaktum("egennaering.fangstogfiske", false).not()

internal fun SøknadFakta.harEøsArbeidsforhold(): Boolean =
    this.getBooleanFaktum("eosarbeidsforhold.jobbetieos", true).not()

internal fun SøknadFakta.harAvtjentVerneplikt(): Boolean =
    this.getFakta("ikkeavtjentverneplikt").getOrNull(0)?.get("value")?.asBoolean()?.not() ?: false
