package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor

class NyttSøknadFormat(private val data: JsonNode) : RutingOppslag {
    override fun eøsBostedsland(): Boolean =
        data
            .hentFaktaFraSeksjon("bostedsland")
            .faktaSvar("faktum.hvilket-land-bor-du-i").asText().erEøsLand()

    override fun eøsArbeidsforhold(): Boolean =
        data.hentFaktaFraSeksjon("eos-arbeidsforhold")
            .faktaSvar("faktum.eos-arbeid-siste-36-mnd").asBoolean()

    override fun avtjentVerneplikt(): Boolean =
        data.hentFaktaFraSeksjon("verneplikt")
            .faktaSvar("faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd").asBoolean()

    override fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
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

    override fun data(): JsonNode {
        TODO("Not yet implemented")
    }

    override fun accept(visitor: SøknadVisitor) {
        TODO("Not yet implemented")
    }
}

private fun JsonNode.hentFaktaFraSeksjon(navn: String) =
    this["seksjoner"].single { it["beskrivendeId"].asText() == navn }["fakta"]

private fun JsonNode.faktaSvar(navn: String) =
    this.single { it["beskrivendeId"].asText() == navn }["svar"]

private fun String.erEøsLand(): Boolean = eøsLandOgSveits.contains(this)

private val eøsLandOgSveits = listOf(
    "BEL",
    "BGR",
    "DNK",
    "EST",
    "FIN",
    "FRA",
    "GRC",
    "IRL",
    "ISL",
    "ITA",
    "HRV",
    "CYP",
    "LVA",
    "LIE",
    "LTU",
    "LUX",
    "MLT",
    "NLD",
    "POL",
    "PRT",
    "ROU",
    "SVK",
    "SVN",
    "ESP",
    "CHE",
    "SWE",
    "CZE",
    "DEU",
    "HUN",
    "AUT"
)
