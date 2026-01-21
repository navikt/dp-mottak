package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor

class BrukerdialogSøknadFormat(private val data: JsonNode) : RutingOppslag {
    private val verdi: JsonNode = data.path("@løsning").path("Søknadsdata").path("verdi")

    init {
        require(verdi.isObject) {
            "Data er ikke i forventet brukerdialog søknadsformat"
        }
    }

    override fun data(): JsonNode = data

    override fun accept(visitor: SøknadVisitor) {
        visitor.visitSøknad(this)
    }

    override fun eøsBostedsland(): Boolean = verdi["eøsBostedsland"]?.asBoolean() ?: false

    override fun eøsArbeidsforhold(): Boolean = verdi["eøsArbeidsforhold"]?.asBoolean() ?: false

    override fun avtjentVerneplikt(): Boolean = verdi["avtjentVerneplikt"]?.asBoolean() ?: false

    override fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
        return verdi["avsluttetArbeidsforhold"]?.map {
            AvsluttetArbeidsforhold(
                sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.valueOf(it["sluttårsak"].asText()),
                fiskeforedling = it["fiskeforedling"]?.asBoolean() ?: false,
                land = it["land"].asText(),
            )
        } ?: emptyList()
    }

    override fun søknadId(): String? = verdi["søknadId"]?.asText()
}
