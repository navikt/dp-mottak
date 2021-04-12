package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode

interface Søknad {
    fun getFakta(faktaNavn: String): List<JsonNode>
    fun getBooleanFaktum(faktaNavn: String): Boolean
    fun getBooleanFaktum(faktaNavn: String, defaultValue: Boolean): Boolean
    fun getChildFakta(faktumId: Int): List<JsonNode>

    companion object {
        fun fromJson(søknadAsJson: JsonNode): Søknad {
            return object : Søknad {
                override fun getFakta(faktaNavn: String): List<JsonNode> =
                    søknadAsJson.get("fakta")?.filter { it["key"].asText() == faktaNavn } ?: emptyList()

                override fun getBooleanFaktum(faktaNavn: String) = getFaktumValue(
                    getFakta(faktaNavn)
                ).asBoolean()

                override fun getBooleanFaktum(faktaNavn: String, defaultValue: Boolean) = kotlin.runCatching {
                    getFaktumValue(
                        getFakta(faktaNavn)
                    ).asBoolean()
                }.getOrDefault(defaultValue)

                override fun getChildFakta(faktumId: Int): List<JsonNode> =
                    søknadAsJson.get("fakta").filter { it["parrentFaktum"].asInt() == faktumId }

                private fun getFaktumValue(fakta: List<JsonNode>): JsonNode = fakta
                    .first()
                    .get("value")
            }
        }
    }
}

internal typealias AvsluttedeArbeidsforhold = List<AvsluttetArbeidsforhold>

fun Søknad.avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
    return this.getFakta("arbeidsforhold").map {
        AvsluttetArbeidsforhold(
            sluttårsak = asÅrsak(it["properties"]["type"].asText()),
            grensearbeider = !this.getBooleanFaktum("arbeidsforhold.grensearbeider", true),
            fiskeforedling = it["properties"]["fangstogfiske"]?.asBoolean() ?: false
        )
    }
}

fun Søknad.erGrenseArbeider(): Boolean =
    this.avsluttetArbeidsforhold().any { it.grensearbeider }

fun Søknad.harAvsluttetArbeidsforholdFraKonkurs(): Boolean =
    this.avsluttetArbeidsforhold().any { it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS }

fun Søknad.erPermittertFraFiskeForedling(): Boolean =
    this.avsluttetArbeidsforhold().any { it.fiskeforedling }

fun Søknad.erFornyetRettighet(): Boolean = this.getBooleanFaktum("fornyetrett.onskerutvidelse", true).not()

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

internal fun Søknad.harInntektFraFangstOgFiske(): Boolean =
    this.getBooleanFaktum("egennaering.fangstogfiske", false).not() ?: false

internal fun Søknad.harEøsArbeidsforhold(): Boolean =
    this.getBooleanFaktum("eosarbeidsforhold.jobbetieos", true).not() ?: false

internal fun Søknad.harAvtjentVerneplikt(): Boolean =
    this.getFakta("ikkeavtjentverneplikt").getOrNull(0)?.get("value")?.asBoolean()?.not() ?: false
