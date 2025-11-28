package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold.Sluttårsak
import no.nav.dagpenger.mottak.QuizOppslag
import no.nav.dagpenger.mottak.ReellArbeidsSøker
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor
import java.time.LocalDate

private val logger = KotlinLogging.logger { }

class OrkestratorSøknadFormat(private val data: JsonNode) : RutingOppslag, QuizOppslag {
    override fun data(): JsonNode = data

    override fun accept(visitor: SøknadVisitor) {
        visitor.visitSøknad(this)
    }

    override fun eøsBostedsland(): Boolean {
        data.hentSeksjonData("personalia").let { personalia ->
            val adresseErINorge = personalia.get("folkeregistrertAdresseErNorgeStemmerDet")?.asText()
            if (adresseErINorge == "ja") {
                return false
            }
            return personalia.get("bostedsland")?.asText()?.erEøsLand() ?: false
        }
    }

    override fun eøsArbeidsforhold(): Boolean {
        return data.hentSeksjonData("arbeidsforhold").let {
            it.faktaSvar("harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene").asText() == "ja"
        }
    }

    override fun avtjentVerneplikt(): Boolean {
        return data.hentSeksjonData("verneplikt").let {
            it.faktaSvar("avtjentVerneplikt").asText() == "ja"
        }
    }

    override fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
        val seksjonsData = data.hentSeksjonData("arbeidsforhold")
        val registrerteArbeidsforhold = seksjonsData.findPath("registrerteArbeidsforhold") ?: emptyList()

        return registrerteArbeidsforhold.filterNot { it.isEmpty }.mapNotNull {
            runCatching {
                AvsluttetArbeidsforhold(
                    sluttårsak = it.sluttårsak(),
                    fiskeforedling = it.fiskForedling(),
                    land = it.faktaSvar("hvilketLandJobbetDuI").asText(),
                )
            }.onFailure { exception ->
                logger.info(exception) { "Klarte ikke å finne AvsluttetArbeidsforhold" }
            }.getOrNull()
        }
    }

    override fun harBarn(): Boolean {
        val barnetilleggSeksjonsData = data.hentSeksjonData("barnetillegg")
        val barnFraPdl = barnetilleggSeksjonsData.single()["harDuBarnFraFolkeregisteret"].isEmpty
        val barnLagtManuelt = barnetilleggSeksjonsData.single()["barnLagtManuelt"].isEmpty

        return !(barnFraPdl && barnLagtManuelt)
    }

    override fun harAndreYtelser(): Boolean {
        val annenPengestøtteSeksjonsData = data.hentSeksjonData("annen-pengestotte")
        val mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav = annenPengestøtteSeksjonsData.faktaSvar("mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav").asText() == "ja"
        val harMottattEllerSøktOmPengestøtteFraAndreEøsLand = annenPengestøtteSeksjonsData.faktaSvar("harMottattEllerSøktOmPengestøtteFraAndreEøsLand").asText() == "ja"
        val fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver = annenPengestøtteSeksjonsData.faktaSvar("fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver").asText() == "ja"

        return mottarDuEllerHarDuSøktOmPengestøtteFraAndreEnnNav ||
            harMottattEllerSøktOmPengestøtteFraAndreEøsLand ||
            fårEllerKommerTilÅFåLønnEllerAndreGoderFraTidligereArbeidsgiver
    }

    override fun permittertFraFiskeForedling(): Boolean {
        return avsluttetArbeidsforhold().any { it.fiskeforedling }
    }

    override fun avsluttetArbeidsforholdFraKonkurs(): Boolean {
        return avsluttetArbeidsforhold().any { it.sluttårsak == Sluttårsak.ARBEIDSGIVER_KONKURS }
    }

    override fun permittert(): Boolean {
        return avsluttetArbeidsforhold().any { it.sluttårsak == Sluttårsak.PERMITTERT }
    }

    override fun fangstOgFisk(): Boolean {
        // todo remove behov from quiz
        return false
    }

    override fun ønskerDagpengerFraDato(): LocalDate {
        val dinSituasjonSeksjonsData = data.hentSeksjonData("din-situasjon")
        val mottattDagpengerFraNavTidligere = dinSituasjonSeksjonsData.faktaSvar("harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene").asText() == "ja"
        return if (mottattDagpengerFraNavTidligere) {
            dinSituasjonSeksjonsData.faktaSvar("hvilkenDatoSøkerDuGjenopptakFra").asLocalDate()
        } else {
            dinSituasjonSeksjonsData.faktaSvar("hvilkenDatoSøkerDuDagpengerFra").asLocalDate()
        }
    }

    override fun søknadId(): String? {
        return data.get("søknad_uuid").asText()
    }

    override fun reellArbeidsSøker(): ReellArbeidsSøker {
        return data.hentSeksjonData("reell-arbeidssoker").let { seksjon ->
            ReellArbeidsSøker(
                helse = seksjon.faktaSvar("kanDuTaAlleTyperArbeid").asText() == "ja",
                geografi = seksjon.faktaSvar("kanDuJobbeIHeleNorge").asText() == "ja",
                deltid = seksjon.faktaSvar("kanDuJobbeBådeHeltidOgDeltid").asText() == "ja",
                yrke = seksjon.faktaSvar("erDuVilligTilÅBytteYrkeEllerGåNedILønn").asText() == "ja",
            )
        }
    }
}

private fun JsonNode.hentSeksjonData(navn: String): JsonNode {
    val seksjonsSvarAsText =
        this["seksjoner"].get(navn).asText() ?: throw NoSuchElementException(
            "Finner ikke seksjon med navn: $navn, seksjoner: ${this["seksjoner"]}",
        )
    return jacksonObjectMapper().readTree(seksjonsSvarAsText)
}

private fun JsonNode.faktaSvar(navn: String): JsonNode =
    this.findPath(navn) ?: throw NoSuchElementException("Finner ikke faktum med navn: $navn")

private fun JsonNode.asLocalDate(): LocalDate = this.asText().let { LocalDate.parse(it) }

private fun JsonNode.fiskForedling(): Boolean =
    this.findPath("permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien").asText() == "ja"

private fun JsonNode.sluttårsak(): Sluttårsak =
    this.findPath("hvordanHarDetteArbeidsforholdetEndretSeg").asText().let {
        when (it) {
            "arbeidsforholdetErIkkeEndret" -> Sluttårsak.IKKE_ENDRET
            "jegHarFåttAvskjed" -> Sluttårsak.AVSKJEDIGET
            "arbeidsgiverenMinHarSagtMegOpp" -> Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
            "arbeidsgiverErKonkurs" -> Sluttårsak.ARBEIDSGIVER_KONKURS
            "kontraktenErUtgått" -> Sluttårsak.KONTRAKT_UTGAATT
            "jegHarSagtOppSelv" -> Sluttårsak.SAGT_OPP_SELV
            "arbeidstidenErRedusert" -> Sluttårsak.REDUSERT_ARBEIDSTID
            "jegErPermitert" -> Sluttårsak.PERMITTERT
            else -> throw IllegalArgumentException("Ukjent sluttårsak: $it")
        }
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
