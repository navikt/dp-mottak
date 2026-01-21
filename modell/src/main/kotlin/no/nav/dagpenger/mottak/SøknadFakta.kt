package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold.Sluttårsak

interface SøknadOppslag {
    fun data(): JsonNode

    fun accept(visitor: SøknadVisitor)

    fun eøsBostedsland(): Boolean

    fun eøsArbeidsforhold(): Boolean

    fun avtjentVerneplikt(): Boolean

    fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold

    fun harBarn(): Boolean

    fun harAndreYtelser(): Boolean

    fun søknadId(): String?
}

interface RutingOppslag : SøknadOppslag {
    fun permittertFraFiskeForedling(): Boolean = avsluttetArbeidsforhold().any { it.fiskeforedling }

    fun avsluttetArbeidsforholdFraKonkurs(): Boolean =
        avsluttetArbeidsforhold().any {
            it.sluttårsak == Sluttårsak.ARBEIDSGIVER_KONKURS
        }

    fun permittert(): Boolean = avsluttetArbeidsforhold().any { it.sluttårsak == Sluttårsak.PERMITTERT }
}

typealias AvsluttedeArbeidsforhold = List<AvsluttetArbeidsforhold>

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
        ARBEIDSGIVER_KONKURS,
        AVSKJEDIGET,
        IKKE_ENDRET,
        KONTRAKT_UTGAATT,
        PERMITTERT,
        PERMITTERT_FISKEFOREDLING,
        REDUSERT_ARBEIDSTID,
        SAGT_OPP_AV_ARBEIDSGIVER,
        SAGT_OPP_SELV,
    }
}
