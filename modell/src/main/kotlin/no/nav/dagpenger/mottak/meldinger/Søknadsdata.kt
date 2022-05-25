package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.AvsluttedeArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.SøknadFaktum
import no.nav.dagpenger.mottak.SøknadVisitor
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.format.DateTimeParseException

class Søknadsdata(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val data: JsonNode
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun søknad(): GammelSøknad = GammelSøknad(data)

    class GammelSøknad(
        val data: JsonNode
    ) : SøknadFaktum {
        override fun søknadsId(): String? = data["brukerBehandlingId"].textValue()

        private fun getFakta(faktaNavn: String): List<JsonNode> =
            data.get("fakta")?.filter { it["key"].asText() == faktaNavn } ?: emptyList()

        private fun getBooleanFaktum(faktaNavn: String) = getFaktumValue(
            getFakta(faktaNavn)
        ).asBoolean()

        private fun getBooleanFaktum(faktaNavn: String, defaultValue: Boolean) = kotlin.runCatching {
            getFaktumValue(
                getFakta(faktaNavn)
            ).asBoolean()
        }.getOrDefault(defaultValue)

        private fun getChildFakta(faktumId: Int): List<JsonNode> =
            data.get("fakta").filter { it["parrentFaktum"].asInt() == faktumId }

        private fun getField(navn: String): JsonNode = data.get(navn)

        private fun getFaktumValue(fakta: List<JsonNode>): JsonNode = fakta
            .first()
            .get("value")

        fun accept(visitor: SøknadVisitor) {
            visitor.visitSøknad(this)
        }

        override fun avsluttetArbeidsforhold(): AvsluttedeArbeidsforhold {
            return this.getFakta("arbeidsforhold")
                .map {
                    AvsluttetArbeidsforhold(
                        sluttårsak = asÅrsak(it["properties"]["type"].asText()),
                        fiskeforedling = it["properties"]["fangstogfiske"]?.asBoolean() ?: false,
                        land = it["properties"]["land"].asText()
                    )
                }
        }

        override fun jobbetUtenforNorge(): Boolean = this.avsluttetArbeidsforhold().any { it.land != "NOR" }

        override fun avsluttetArbeidsforholdFraKonkurs(): Boolean =
            this.avsluttetArbeidsforhold()
                .any { it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS }

        override fun permittertFraFiskeForedling(): Boolean = this.avsluttetArbeidsforhold().any { it.fiskeforedling }
        override fun permittert(): Boolean =
            this.avsluttetArbeidsforhold().any { it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT }

        override fun eøsBostedsland(): Boolean =
            this.getFakta("bostedsland.land").any { it["value"].asText() in eøsLand }

        override fun eøsArbeidsforhold(): Boolean = this.getBooleanFaktum("eosarbeidsforhold.jobbetieos", true).not()

        override fun avtjentVerneplikt(): Boolean =
            this.getFakta("ikkeavtjentverneplikt").getOrNull(0)?.get("value")?.asBoolean()?.not() ?: false

        override fun helseTilAlleTyperJobb(): Boolean = getBooleanFaktum("reellarbeidssoker.villighelse")

        override fun kanJobbeHvorSomHelst(): Boolean =
            getBooleanFaktum("reellarbeidssoker.villigpendle")

        override fun kanJobbeDeltid(): Boolean = getBooleanFaktum("reellarbeidssoker.villigdeltid")

        override fun villigTilÅBytteYrke(): Boolean = getBooleanFaktum("reellarbeidssoker.villigjobb")

        override fun rettighetstypeUtregning(): List<Map<String, Boolean>> =
            rettighetstypeUtregning(this.avsluttetArbeidsforhold())

        private fun rettighetstypeUtregning(avsluttedeArbeidsforhold: List<AvsluttetArbeidsforhold>) =
            avsluttedeArbeidsforhold.map {
                mapOf(
                    "Lønnsgaranti" to (it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS),
                    "PermittertFiskeforedling" to (it.fiskeforedling),
                    "Permittert" to (it.sluttårsak == AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT && !it.fiskeforedling),
                    "Ordinær" to (
                        it.sluttårsak != AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT &&
                            it.sluttårsak != AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS &&
                            !it.fiskeforedling
                        )
                )
            }

        override fun harJobbetIeøsOmråde(): Boolean =
            getBooleanFaktum("eosarbeidsforhold.jobbetieos", true).not()

        override fun fangstOgFisk(): Boolean = getBooleanFaktum("egennaering.fangstogfiske").not()

        override fun verneplikt(): Boolean = getBooleanFaktum("ikkeavtjentverneplikt", true).not()

        override fun ønskerDagpengerFraDato(): LocalDate =
            getFakta("arbeidsforhold.datodagpenger").first()["value"].asLocalDate()

        override fun søknadstidspunkt(): LocalDate =
            getFakta("innsendtDato").first()["value"].asLocalDateTime().toLocalDate()

        override fun sisteDagMedLønnEllerArbeidsplikt(): LocalDate {
            if (getFakta("arbeidsforhold").isEmpty())
                return getFakta("arbeidsforhold.datodagpenger").first()["value"].asLocalDate()
            return when (avsluttetArbeidsforhold().first().sluttårsak) {
                AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS -> sisteDagMedLønnKonkurs()
                else -> sisteDagMedLønnEllerArbeidspliktResten()
            }
        }

        override fun sisteDagMedLønnKonkurs(): LocalDate {
            return getFakta("arbeidsforhold").first().let {
                localDateEllerNull(it["properties"]["lonnkonkursmaaned_dato"])
                    ?: it["properties"]["konkursdato"].asLocalDate()
            }
        }

        override fun sisteDagMedLønnEllerArbeidspliktResten(): LocalDate {
            return getFakta("arbeidsforhold").first().let {
                localDateEllerNull(it["properties"]["datotil"])
                    ?: localDateEllerNull(it["properties"]["lonnspliktigperiodedatotil"])
                    ?: localDateEllerNull(it["properties"]["redusertfra"])
                    ?: getFakta("arbeidsforhold.permitteringsperiode")
                        .first()["properties"]["permiteringsperiodedatofra"].asLocalDate()
            }
        }

        private fun asÅrsak(type: String): AvsluttetArbeidsforhold.Sluttårsak = when (type) {
            "permittert" -> AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT
            "avskjediget" -> AvsluttetArbeidsforhold.Sluttårsak.AVSKJEDIGET
            "kontraktutgaatt" -> AvsluttetArbeidsforhold.Sluttårsak.KONTRAKT_UTGAATT
            "redusertarbeidstid" -> AvsluttetArbeidsforhold.Sluttårsak.REDUSERT_ARBEIDSTID
            "sagtoppavarbeidsgiver" -> AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
            "sagtoppselv" -> AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_SELV
            "arbeidsgivererkonkurs" -> AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS
            "ikke-endret" -> AvsluttetArbeidsforhold.Sluttårsak.IKKE_ENDRET
            else -> throw Exception("Missing permitteringstype: $type")
        }

        private val eøsLand = setOf(
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
            "GBR",
            "CHE",
            "SWE",
            "CZE",
            "DEU",
            "HUN",
            "AUT"
        )
    }
}

private fun JsonNode.asLocalDateTime(): LocalDateTime = this.asText().let { LocalDateTime.parse(it) }

private fun JsonNode.asLocalDate(): LocalDate = this.asText().let { LocalDate.parse(it) }

private fun localDateEllerNull(jsonNode: JsonNode?): LocalDate? = jsonNode?.let {
    try {
        LocalDate.parse(jsonNode.asText())
    } catch (e: DateTimeParseException) { // Optional datoer får noen ganger verdi “NaN-aN-aN”
        null
    }
}
