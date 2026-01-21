package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor

class BrukerdialogSøknadFormat(private val data: JsonNode) : RutingOppslag {
    override fun data(): JsonNode {
        TODO("Not yet implemented")
    }

    override fun accept(visitor: SøknadVisitor) {
        TODO("Not yet implemented")
    }

    override fun eøsBostedsland(): Boolean {
        TODO("Not yet implemented")
    }

    override fun eøsArbeidsforhold(): Boolean {
        TODO("Not yet implemented")
    }

    override fun avtjentVerneplikt(): Boolean {
        TODO("Not yet implemented")
    }

    override fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
        TODO("Not yet implemented")
    }

    override fun harBarn(): Boolean {
        TODO("Not yet implemented")
    }

    override fun harAndreYtelser(): Boolean {
        TODO("Not yet implemented")
    }

    override fun søknadId(): String? {
        TODO("Not yet implemented")
    }

    override fun permittertFraFiskeForedling(): Boolean {
        TODO("Not yet implemented")
    }

    override fun avsluttetArbeidsforholdFraKonkurs(): Boolean {
        TODO("Not yet implemented")
    }

    override fun permittert(): Boolean {
        TODO("Not yet implemented")
    }
}
