package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

interface SøknadFakta {
    fun eøsBostedsland(): Boolean
    fun eøsArbeidsforhold(): Boolean
    fun avtjentVerneplikt(): Boolean
    fun fangstOgFisk(): Boolean
    fun ønskerDagpengerFraDato(): LocalDate
    fun søknadstidspunkt(): LocalDate
    fun sisteDagMedLønnEllerArbeidsplikt(): LocalDate
    fun sisteDagMedLønnKonkurs(): LocalDate
    fun sisteDagMedLønnEllerArbeidspliktResten(): LocalDate
    fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold
    fun søknadsId(): String?
    fun reellArbeidsSøker(): ReellArbeidsSøker
    fun asJson(): JsonNode
    fun accept(visitor: SøknadVisitor)
}

internal fun SøknadFakta.avsluttetArbeidsforholdFraKonkurs(): Boolean =
    this.avsluttetArbeidsforhold()
        .any { it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS }

internal fun SøknadFakta.permittertFraFiskeForedling(): Boolean =
    this.avsluttetArbeidsforhold().any { it.fiskeforedling }

internal fun SøknadFakta.permittert(): Boolean =
    this.avsluttetArbeidsforhold().any { it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT }

internal typealias AvsluttedeArbeidsforhold = List<AvsluttetArbeidsforhold>

data class ReellArbeidsSøker(
    val helse: Boolean,
    val geografi: Boolean,
    val deltid: Boolean,
    val yrke: Boolean
)

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
