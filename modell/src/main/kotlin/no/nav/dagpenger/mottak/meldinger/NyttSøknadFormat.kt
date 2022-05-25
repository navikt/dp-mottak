package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.ReellArbeidsSøker
import no.nav.dagpenger.mottak.SøknadFakta
import no.nav.dagpenger.mottak.SøknadVisitor
import java.time.LocalDate

class NyttSøknadFormat(private val data: JsonNode) : SøknadFakta {
    override fun eøsBostedsland(): Boolean {
        TODO("Not yet implemented")
    }

    override fun eøsArbeidsforhold(): Boolean {
        TODO("Not yet implemented")
    }

    override fun avtjentVerneplikt(): Boolean {
        TODO("Not yet implemented")
    }

    override fun fangstOgFisk(): Boolean {
        TODO("Not yet implemented")
    }

    override fun ønskerDagpengerFraDato(): LocalDate {
        TODO("Not yet implemented")
    }

    override fun søknadstidspunkt(): LocalDate {
        TODO("Not yet implemented")
    }

    override fun sisteDagMedLønnEllerArbeidsplikt(): LocalDate {
        TODO("Not yet implemented")
    }

    override fun sisteDagMedLønnKonkurs(): LocalDate {
        TODO("Not yet implemented")
    }

    override fun sisteDagMedLønnEllerArbeidspliktResten(): LocalDate {
        TODO("Not yet implemented")
    }

    override fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
        TODO("Not yet implemented")
    }

    override fun søknadsId(): String? {
        TODO("Not yet implemented")
    }

    override fun reellArbeidsSøker(): ReellArbeidsSøker {
        TODO("Not yet implemented")
    }

    override fun asJson(): JsonNode {
        TODO("Not yet implemented")
    }

    override fun accept(visitor: SøknadVisitor) {
        TODO("Not yet implemented")
    }
}
