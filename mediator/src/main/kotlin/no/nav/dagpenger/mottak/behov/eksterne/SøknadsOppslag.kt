package no.nav.dagpenger.mottak.behov.eksterne

import java.time.LocalDate

internal interface SøknadsOppslag {
    fun hentSøknad(journalpostId: String): Søknad
}

internal class Søknad {
    fun ønskerDagpengerFraDato(): LocalDate {
        TODO("Not yet implemented")
    }

    fun søknadstidspunkt(): LocalDate {
        TODO("Not yet implemented")
    }

    fun verneplikt(): Boolean {
        TODO("Not yet implemented")
    }

    fun fangstOgFisk(): Boolean {
        TODO("Not yet implemented")
    }

    fun jobbetIeøs(): Boolean {
        TODO("Not yet implemented")
    }

    fun arbeidspliktEllerLønnsplikt(): Boolean {
        TODO("Not yet implemented")
    }

    fun lærling(): Boolean {
        TODO("Not yet implemented")
    }

    fun rettighetstype(): AvsluttetArbeidsforhold.Sluttårsak {
        TODO("Not yet implemented")
    }

    fun sisteDagMedArbeidsplikt(): LocalDate {
        TODO("Not yet implemented")
    }

    fun sisteDagMedLønn(): Any {
        TODO("Not yet implemented")
    }
}

data class AvsluttetArbeidsforhold(
    val sluttårsak: Sluttårsak,
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
