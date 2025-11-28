package no.nav.dagpenger.mottak.meldinger
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mottak.meldinger.søknadsdata.OrkestratorSøknadFormat
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.util.UUID
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class OrkestratorSøknadFormatTest {
    @Test
    fun `test om bostedsland er i EØS`() {
        assertFalse(OrkestratorSøknadFormat(personaliaOrkestratorJson("ja", "NOR")).eøsBostedsland())
        assertTrue(OrkestratorSøknadFormat(personaliaOrkestratorJson("nei", "POL")).eøsBostedsland())
        assertFalse(OrkestratorSøknadFormat(personaliaOrkestratorJson("nei", "AGO")).eøsBostedsland())
    }

    @Test
    fun `har eøs arbeidsforhold`() {
        assertTrue(OrkestratorSøknadFormat(eøsArbeidsforholdOrkestratorJson("ja")).eøsArbeidsforhold())
        assertFalse(OrkestratorSøknadFormat(eøsArbeidsforholdOrkestratorJson("nei")).eøsArbeidsforhold())
    }

    @Test
    fun `har avtjent verneplikt`() {
        assertTrue(OrkestratorSøknadFormat(avtjentVernepliktOrkestratorJson("ja")).avtjentVerneplikt())
        assertFalse(OrkestratorSøknadFormat(avtjentVernepliktOrkestratorJson("nei")).avtjentVerneplikt())
    }

    @Test
    fun `kan parse avslutte arbeidsforhold`() {
        OrkestratorSøknadFormat(arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()).avsluttetArbeidsforhold().size shouldBe 2
        OrkestratorSøknadFormat(arbeidsforholdUtenRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()).avsluttetArbeidsforhold().size shouldBe 0
        OrkestratorSøknadFormat(arbeidsforholdDelvisUtfylteRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()).avsluttetArbeidsforhold().size shouldBe 1
    }

    @Test
    fun `er permitert fra fiske foredling`() {
        OrkestratorSøknadFormat(arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()).permittertFraFiskeForedling() shouldBe true
        OrkestratorSøknadFormat(arbeidsforholdDelvisUtfylteRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()).permittertFraFiskeForedling() shouldBe false
    }

    @Test
    fun `avsluttet arbeidsforhold fra arbeidsgiver konkurs`() {
        OrkestratorSøknadFormat(arbeidsforholdAvsluttedeArbeidsforholdPgaKonkursOrkestratorJson()).avsluttetArbeidsforholdFraKonkurs() shouldBe true
        OrkestratorSøknadFormat(arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson()).avsluttetArbeidsforholdFraKonkurs() shouldBe false
    }

    @Test
    fun `avsluttet arbeidsforhold pga permittert`() {
        OrkestratorSøknadFormat(arbeidsforholdAvsluttedeArbeidsforholdPgaKonkursOrkestratorJson()).permittert() shouldBe false
        OrkestratorSøknadFormat(arbeidsforholdAvsluttedeArbeidsforholdPgaPermitertOrkestratorJson()).permittert() shouldBe true
    }

    @Test
    fun `ønsker gjenopptakelse av dagpenger`() {
        val now = LocalDate.now()
        OrkestratorSøknadFormat(dinSituasjonMedGjenopptakelseOrkestratorJson(now)).ønskerDagpengerFraDato() shouldBe now
    }

    @Test
    fun `ønsker start av dagpenger`() {
        val now = LocalDate.now()
        OrkestratorSøknadFormat(dinSituasjonUtenGjenopptakelseOrkestratorJson(now)).ønskerDagpengerFraDato() shouldBe now
    }

    @Test
    fun `hent SøknadId fra dataklasse`() {
        val uuid = UUID.randomUUID()
        val orkestratorSøknadFormat = OrkestratorSøknadFormat(dataklasseOrkestratorJson(uuid))
        orkestratorSøknadFormat.søknadId().toString() shouldBe uuid.toString()
    }
}

fun personaliaOrkestratorJson(
    borINorge: String,
    bostedsland: String,
): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "personalia": "{\"fornavnFraPdl\":\"TOPP\",\"mellomnavnFraPdl\":\"\",\"etternavnFraPdl\":\"SURE\",\"fødselsnummerFraPdl\":\"21857998666\",\"alderFraPdl\":\"46\",\"adresselinje1FraPdl\":\"Dale 17\",\"adresselinje2FraPdl\":\"\",\"adresselinje3FraPdl\":\"\",\"postnummerFraPdl\":\"9423\",\"poststedFraPdl\":\"Grøtavær\",\"landkodeFraPdl\":\"NO\",\"landFraPdl\":\"NORGE\",\"kontonummerFraKontoregister\":\"\",\"folkeregistrertAdresseErNorgeStemmerDet\":\"$borINorge\",\"bostedsland\":\"$bostedsland\"}"
          }
        }
        """.trimIndent(),
    )
}

fun eøsArbeidsforholdOrkestratorJson(
    eøsArbeidsforhold: String,
): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "arbeidsforhold": "{\"seksjonId\":\"arbeidsforhold\",\"seksjonsvar\":{\"hvordanHarDuJobbet\":\"fastArbeidstidIMindreEnn6Måneder\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"$eøsArbeidsforhold\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"asdasd\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2024-01-01\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-27\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"arbeidsgiverenMinHarSagtMegOpp\",\"jegErOppsagtHvaVarÅrsaken\":\"sdfsfd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"f047539f-6911-4902-9af5-f1b85545496c\",\"dokumentasjonskrav\":[\"533bdc6d-a3ba-4936-ace2-cd455aaf86ab\",\"a0ec261c-631e-4d2e-8c92-ecabcd399eab\"]}]},\"versjon\":1}"
          }
        }
        """.trimIndent(),
    )
}

fun avtjentVernepliktOrkestratorJson(
    avtjentVerneplikt: String,
): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "verneplikt": "{\"avtjentVerneplikt\":\"$avtjentVerneplikt\",\"dokumentasjonskrav\":\"null\"}"
          }
        }
        """.trimIndent(),
    )
}

fun arbeidsforholdMedRegistrerteAvsluttedeArbeidsforholdOrkestratorJson(): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "arbeidsforhold": "{\"hvordanHarDuJobbet\":\"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"nei\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"Oslo burger og strøm\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-19\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-28\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"arbeidsgiverenMinHarSagtMegOpp\",\"jegErOppsagtHvaVarÅrsaken\":\"asd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"1c01dec1-0738-4ed9-a6fb-fb75a0730302\",\"dokumentasjonskrav\":[\"6bf2a891-5be3-4477-8955-4c0c488735db\",\"c23b0fa4-e4ad-4734-b780-66746c4bd3b2\"]},{\"navnetPåBedriften\":\"LAVE HESTER AS\",\"hvilketLandJobbetDuI\":\"SWE\",\"oppgiPersonnummeretPinDuHaddeIDetteLandet\":\"12431441\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-05\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"jegErPermitert\",\"permittertErDetteEtMidlertidigArbeidsforholdMedEnKontraktfestetSluttdato\":\"ja\",\"permittertOppgiDenKontraktsfestedeSluttdatoenPåDetteArbeidsforholdet\":\"2025-11-03\",\"permittertNårStartetDuIDenneJobben\":\"2025-11-27\",\"permittertErDuPermittertFraFiskeforedlingsEllerFiskeoljeindustrien\":\"ja\",\"permittertNårErDuPermittertFraOgMedDato\":\"2025-11-03\",\"permittertNårErDuPermittertTilOgMedDato\":\"2025-11-29\",\"permittertHvorMangeProsentErDuPermittert\":\"12\",\"permittertVetDuNårLønnspliktperiodenTilArbeidsgiverenDinEr\":\"ja\",\"permittertLønnsperiodeFraOgMedDato\":\"2025-11-24\",\"permittertLønnsperiodeTilOgMedDato\":\"2025-11-30\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"rotasjon\",\"hvilkenTypeRotasjonsordningJobbetDu\":\"2-3-rotasjon\",\"oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinDatoFraOgMed\":\"2025-11-12\",\"oppgiSisteArbeidsperiodeIDenSisteRotasjonenDinDatoTilOgMed\":\"2025-11-28\"}]}"
          }
        }
        """.trimIndent(),
    )
}

fun arbeidsforholdUtenRegistrerteAvsluttedeArbeidsforholdOrkestratorJson(): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "arbeidsforhold": "{\"hvordan-har-du-jobbet\":\"har-ikke-jobbet-de-siste-36-månedene\",\"registrerteArbeidsforhold\":[]}"
          }
        }
        """.trimIndent(),
    )
}

fun arbeidsforholdDelvisUtfylteRegistrerteAvsluttedeArbeidsforholdOrkestratorJson(): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "arbeidsforhold": "{\"hvordanHarDuJobbet\":\"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"nei\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"Oslo burger og strøm\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-19\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-28\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"arbeidsgiverenMinHarSagtMegOpp\",\"jegErOppsagtHvaVarÅrsaken\":\"asd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"1c01dec1-0738-4ed9-a6fb-fb75a0730302\",\"dokumentasjonskrav\":[\"6bf2a891-5be3-4477-8955-4c0c488735db\",\"c23b0fa4-e4ad-4734-b780-66746c4bd3b2\"]}]}"
          }
        }
        """.trimIndent(),
    )
}

fun arbeidsforholdAvsluttedeArbeidsforholdPgaKonkursOrkestratorJson(): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "arbeidsforhold": "{\"hvordanHarDuJobbet\":\"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"nei\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"Oslo burger og strøm\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-19\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-28\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"arbeidsgiverErKonkurs\",\"jegErOppsagtHvaVarÅrsaken\":\"asd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"1c01dec1-0738-4ed9-a6fb-fb75a0730302\",\"dokumentasjonskrav\":[\"6bf2a891-5be3-4477-8955-4c0c488735db\",\"c23b0fa4-e4ad-4734-b780-66746c4bd3b2\"]}]}"
          }
        }
        """.trimIndent(),
    )
}

fun arbeidsforholdAvsluttedeArbeidsforholdPgaPermitertOrkestratorJson(): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "arbeidsforhold": "{\"hvordanHarDuJobbet\":\"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"nei\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"Oslo burger og strøm\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-19\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-28\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"jegErPermitert\",\"jegErOppsagtHvaVarÅrsaken\":\"asd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"1c01dec1-0738-4ed9-a6fb-fb75a0730302\",\"dokumentasjonskrav\":[\"6bf2a891-5be3-4477-8955-4c0c488735db\",\"c23b0fa4-e4ad-4734-b780-66746c4bd3b2\"]}]}"
          }
        }
        """.trimIndent(),
    )
}

fun dinSituasjonUtenGjenopptakelseOrkestratorJson(now: LocalDate): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "din-situasjon": "{\"harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene\":\"nei\",\"hvilkenDatoSøkerDuDagpengerFra\":\"$now\"}"
          }
        }
        """.trimIndent(),
    )
}

fun dinSituasjonMedGjenopptakelseOrkestratorJson(now: LocalDate): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "din-situasjon": "{\"harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene\": \"ja\",\"årsakTilAtDagpengeneBleStanset\": \"dfg\",\"hvilkenDatoSøkerDuGjenopptakFra\": \"$now\"}"
          }
        }
        """.trimIndent(),
    )
}

fun dataklasseOrkestratorJson(uuid: UUID): JsonNode {
    return jacksonObjectMapper().readTree(
        """
        {
          "seksjoner": {
            "din-situasjon": "{\"harDuMottattDagpengerFraNavILøpetAvDeSiste52Ukene\":\"nei\",\"hvilkenDatoSøkerDuDagpengerFra\":\"2025-11-21\"}",
            "arbeidsforhold": "{\"hvordanHarDuJobbet\":\"jobbetMerIGjennomsnittDeSiste36MånedeneEnnDeSiste12Månedene\",\"harDuJobbetIEtAnnetEøsLandSveitsEllerStorbritanniaILøpetAvDeSiste36Månedene\":\"nei\",\"registrerteArbeidsforhold\":[{\"navnetPåBedriften\":\"Oslo burger og strøm\",\"hvilketLandJobbetDuI\":\"NOR\",\"varighetPåArbeidsforholdetFraOgMedDato\":\"2025-11-19\",\"varighetPåArbeidsforholdetTilOgMedDato\":\"2025-11-28\",\"hvordanHarDetteArbeidsforholdetEndretSeg\":\"jegErPermitert\",\"jegErOppsagtHvaVarÅrsaken\":\"asd\",\"jegErOppsagtHarDuFåttTilbudOmÅFortsetteHosArbeidsgiverenDinIAnnenStillingEllerEtAnnetStedINorge\":\"nei\",\"harDuJobbetSkiftTurnusEllerRotasjon\":\"hverken-skift-turnus-eller-rotasjon\",\"id\":\"1c01dec1-0738-4ed9-a6fb-fb75a0730302\",\"dokumentasjonskrav\":[\"6bf2a891-5be3-4477-8955-4c0c488735db\",\"c23b0fa4-e4ad-4734-b780-66746c4bd3b2\"]}]}"
          },
          "søknad_uuid": "$uuid",
          "fødselsnummer": "21857998666",
          "versjon_navn": "Dagpenger_v2"
          
        }
        """.trimIndent(),
    )
}
