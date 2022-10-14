package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold.Sluttårsak
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor

class QuizSøknadFormat(private val data: JsonNode) : RutingOppslag {
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
        val faktaFraSeksjon = data.hentFaktaFraSeksjon("arbeidsforhold")
        val arbeidsforhold =
            faktaFraSeksjon.singleOrNull { it["beskrivendeId"].asText() == "faktum.arbeidsforhold" }?.get("svar")
                ?: emptyList()
        return arbeidsforhold.map {
            AvsluttetArbeidsforhold(
                sluttårsak = it.sluttårsak(),
                fiskeforedling = it.fiskForedling(),
                land = it.faktaSvar("faktum.arbeidsforhold.land").asText()
            )
        }
    }

    override fun permittertFraFiskeForedling(): Boolean = avsluttetArbeidsforhold().any { it.fiskeforedling }

    override fun avsluttetArbeidsforholdFraKonkurs(): Boolean =
        avsluttetArbeidsforhold().any { it.sluttårsak == Sluttårsak.ARBEIDSGIVER_KONKURS }

    override fun permittert(): Boolean = avsluttetArbeidsforhold().any { it.sluttårsak == Sluttårsak.PERMITTERT }

    override fun data(): JsonNode = data

    override fun accept(visitor: SøknadVisitor) {
        visitor.visitSøknad(this)
    }
}

private fun JsonNode.fiskForedling(): Boolean =
    this.find { it["beskrivendeId"].asText() == "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering" }
        ?.get("svar")?.asBoolean() ?: false

private fun JsonNode.sluttårsak(): Sluttårsak = this.faktaSvar("faktum.arbeidsforhold.endret").asText().let {
    when (it) {
        "faktum.arbeidsforhold.endret.svar.ikke-endret" -> Sluttårsak.IKKE_ENDRET
        "faktum.arbeidsforhold.endret.svar.avskjediget" -> Sluttårsak.AVSKJEDIGET
        "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver" -> Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
        "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs" -> Sluttårsak.ARBEIDSGIVER_KONKURS
        "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt" -> Sluttårsak.KONTRAKT_UTGAATT
        "faktum.arbeidsforhold.endret.svar.sagt-opp-selv" -> Sluttårsak.SAGT_OPP_SELV
        "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid" -> Sluttårsak.REDUSERT_ARBEIDSTID
        "faktum.arbeidsforhold.endret.svar.permittert" -> Sluttårsak.PERMITTERT
        else -> throw IllegalArgumentException("Ukjent sluttårsak: $it")
    }
}

private fun JsonNode.hentFaktaFraSeksjon(navn: String) =
    try {
        this["seksjoner"].single { it["beskrivendeId"].asText() == navn }.get("fakta")
    } catch (e: NoSuchElementException) {
        val seksjoner = this["seksjoner"].map { it["beskrivendeId"].asText() }
        throw NoSuchElementException("Fant ikke seksjon med navn=$navn, seksjoner=$seksjoner", e)
    }

private fun JsonNode.hentNullableFaktaFraSeksjon(navn: String) =
    this["seksjoner"].singleOrNull { it["beskrivendeId"].asText() == navn }?.get("fakta")

private fun JsonNode.faktaSvar(navn: String) =
    try {
        this.single { it["beskrivendeId"].asText() == navn }["svar"]
    } catch (e: NoSuchElementException) {
        val fakta = this.map { it["beskrivendeId"].asText() }
        throw NoSuchElementException("Fant ikke fakta med navn=$navn, fakta=$fakta", e)
    }

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
