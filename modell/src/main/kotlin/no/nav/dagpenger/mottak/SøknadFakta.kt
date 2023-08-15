package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode
import java.time.LocalDate

interface SøknadOppslag {
    fun data(): JsonNode
    fun accept(visitor: SøknadVisitor)
    fun eøsBostedsland(): Boolean
    fun eøsArbeidsforhold(): Boolean
    fun avtjentVerneplikt(): Boolean
    fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold
    fun harBarn(): Boolean
    fun harAndreYtelser(): Boolean
}

interface QuizOppslag : SøknadOppslag {
    fun fangstOgFisk(): Boolean
    fun ønskerDagpengerFraDato(): LocalDate
    fun søknadsId(): String?
    fun reellArbeidsSøker(): ReellArbeidsSøker
}

interface RutingOppslag : SøknadOppslag {
    fun permittertFraFiskeForedling(): Boolean
    fun avsluttetArbeidsforholdFraKonkurs(): Boolean
    fun permittert(): Boolean
}

internal typealias AvsluttedeArbeidsforhold = List<AvsluttetArbeidsforhold>

data class ReellArbeidsSøker(
    val helse: Boolean,
    val geografi: Boolean,
    val deltid: Boolean,
    val yrke: Boolean,
)

data class AvsluttetArbeidsforhold(
    val sluttårsak: Sluttårsak,
    val fiskeforedling: Boolean,
    val land: String,
) {
    enum class Sluttårsak {
        AVSKJEDIGET,
        ARBEIDSGIVER_KONKURS,
        KONTRAKT_UTGAATT,
        PERMITTERT,
        REDUSERT_ARBEIDSTID,
        SAGT_OPP_AV_ARBEIDSGIVER,
        SAGT_OPP_SELV,
        IKKE_ENDRET,
    }
}
