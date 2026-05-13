package no.nav.dagpenger.mottak.meldinger.søknadsdata

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold.Sluttårsak
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor
import tools.jackson.databind.JsonNode

private val logger = KotlinLogging.logger { }

class QuizSøknadFormat(private val data: JsonNode) : RutingOppslag {
    override fun eøsBostedsland(): Boolean =
        data
            .hentNullableFaktaFraSeksjon("bostedsland")
            ?.faktaSvar("faktum.hvilket-land-bor-du-i")?.asString()?.erEøsLand() ?: false

    override fun eøsArbeidsforhold(): Boolean =
        data.hentNullableFaktaFraSeksjon("eos-arbeidsforhold")
            ?.faktaSvar("faktum.eos-arbeid-siste-36-mnd")?.asBoolean() ?: false

    override fun avtjentVerneplikt(): Boolean =
        data.hentNullableFaktaFraSeksjon("verneplikt")
            ?.faktaSvar("faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd")?.asBoolean() ?: false

    override fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
        val faktaFraSeksjon = data.hentNullableFaktaFraSeksjon("din-situasjon")
        val arbeidsforhold =
            faktaFraSeksjon?.values()?.singleOrNull { it["beskrivendeId"].asString() == "faktum.arbeidsforhold" }?.get("svar")
        return (arbeidsforhold?.values() ?: emptyList()).filterNot { it.isEmpty }.mapNotNull {
            kotlin.runCatching {
                AvsluttetArbeidsforhold(
                    sluttårsak = it.sluttårsak(),
                    fiskeforedling = it.fiskForedling(),
                    land = it.faktaSvar("faktum.arbeidsforhold.land").asString(),
                )
            }.onFailure { exception ->
                logger.info(exception) { "Klarte ikke å finne AvsluttetArbeidsforhold" }
            }.getOrNull()
        }
    }

    override fun søknadId(): String? = data["søknad_uuid"]?.stringValue()

    override fun data(): JsonNode = data

    override fun accept(visitor: SøknadVisitor) {
        visitor.visitSøknad(this)
    }
}

private fun JsonNode.fiskForedling(): Boolean =
    this.values().find { it["beskrivendeId"].asString() == "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering" }
        ?.get("svar")?.asBoolean() ?: false

private fun JsonNode.sluttårsak(): Sluttårsak =
    this.faktaSvar("faktum.arbeidsforhold.endret").asString().let {
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

private fun JsonNode.hentNullableFaktaFraSeksjon(navn: String): JsonNode? =
    this["seksjoner"].values().singleOrNull { it["beskrivendeId"].asString() == navn }?.get("fakta")

private fun JsonNode.faktaSvar(navn: String) =
    try {
        this.values().single { it["beskrivendeId"].asString() == navn }["svar"]
    } catch (e: NoSuchElementException) {
        val fakta = this.values().map { it["beskrivendeId"].asString() }
        throw NoSuchElementException("Fant ikke fakta med navn=$navn, fakta=$fakta", e)
    }

private fun String.erEøsLand(): Boolean = eøsLandOgSveits.contains(this)

private val eøsLandOgSveits =
    listOf(
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
        "AUT",
    )
