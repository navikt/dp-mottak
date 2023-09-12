package no.nav.dagpenger.mottak.behov.eksterne

import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold.Sluttårsak
import no.nav.dagpenger.mottak.QuizOppslag
import no.nav.dagpenger.mottak.behov.JsonMapper
import no.nav.dagpenger.mottak.meldinger.søknadsdata.QuizSøknadFormat
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource

internal class SøknadFaktaQuizLøserTest {

    val testRapid = TestRapid()
    val testSøknad =
        QuizSøknadFormat(JsonMapper.jacksonJsonAdapter.readTree(this.javaClass.getResource("/testdata/soknadsdata.json")))

    init {
        SøknadFaktaQuizLøser(
            søknadQuizOppslag = object : SøknadQuizOppslag {
                override fun hentSøknad(innsendtSøknadsId: String): QuizOppslag = testSøknad
            },
            rapidsConnection = testRapid,
        )
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "ØnskerDagpengerFraDato:2023-01-09",
            "Verneplikt:false",
            "FangstOgFiske:false",
            "EØSArbeid:false",
            "KanJobbeDeltid:true",
            "KanJobbeHvorSomHelst:true",
            "HelseTilAlleTyperJobb:true",
            "VilligTilÅBytteYrke:true",
            "JobbetUtenforNorge:false",
        ],
        delimiter = ':',
    )
    fun `besvarer fakta behov`(behovNavn: String, forventetVerdi: String) {
        testRapid.sendTestMessage(behovMelding(behovNavn))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertEquals(forventetVerdi, field(0, "@løsning")[behovNavn].asText())
        }
    }

    @Test
    fun `Bevarer @id i behov`() {
        testRapid.sendTestMessage(behovMelding("Verneplikt"))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertEquals("930e2beb-d394-4024-b713-dbeb6ad3d4bf", field(0, "@behovId").asText())
        }
    }

    @Test
    fun `besvarer meldinger med flere behov enn ett`() {
        testRapid.sendTestMessage(meldingMedFlereBehov())
        assertEquals(1, testRapid.inspektør.size)

        with(testRapid.inspektør) {
            assertEquals("false", field(0, "@løsning")["Verneplikt"].asText())
            assertEquals("false", field(0, "@løsning")["JobbetUtenforNorge"].asText())
        }
    }

    @Test
    fun `mapper avsluttede arbeidsforhold til lønnsgaranti`() {
        rettighetstypeUtregning(
            listOf<AvsluttetArbeidsforhold>(
                AvsluttetArbeidsforhold(
                    sluttårsak = Sluttårsak.ARBEIDSGIVER_KONKURS,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).also {
            assertEquals(1, it.size)
            it[0].assertRettighetstype("Lønnsgaranti")
        }
    }

    @Test
    fun `mapper avsluttede arbeidsforhold til permitert fiskeforedling`() {
        rettighetstypeUtregning(
            listOf<AvsluttetArbeidsforhold>(
                AvsluttetArbeidsforhold(
                    sluttårsak = Sluttårsak.PERMITTERT,
                    fiskeforedling = true,
                    land = "NOR",
                ),
            ),
        ).also {
            assertEquals(1, it.size)
            it[0].assertRettighetstype("PermittertFiskeforedling")
        }
    }

    @Test
    fun `mapper avsluttede arbeidsforhold til permittert`() {
        rettighetstypeUtregning(
            listOf<AvsluttetArbeidsforhold>(
                AvsluttetArbeidsforhold(
                    sluttårsak = Sluttårsak.PERMITTERT,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).also {
            assertEquals(1, it.size)
            it[0].assertRettighetstype("Permittert")
        }
    }

    @Test
    fun `mapper avsluttede arbeidsforhold til ordinær`() {
        rettighetstypeUtregning(
            listOf<AvsluttetArbeidsforhold>(
                AvsluttetArbeidsforhold(
                    sluttårsak = Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).also {
            assertEquals(1, it.size)
            it[0].assertRettighetstype("Ordinær")
        }
    }

    @Test
    fun `mapper flere avsluttede arbeidsforhold til rettoghetstype`() {
        rettighetstypeUtregning(
            listOf<AvsluttetArbeidsforhold>(
                AvsluttetArbeidsforhold(
                    sluttårsak = Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER,
                    fiskeforedling = false,
                    land = "NOR",
                ),
                AvsluttetArbeidsforhold(
                    sluttårsak = Sluttårsak.PERMITTERT,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).also {
            assertEquals(2, it.size)
            it[0].assertRettighetstype("Ordinær")
            it[1].assertRettighetstype("Permittert")
        }
    }

    fun Map<String, Boolean>.assertRettighetstype(nøkkel: String) {
        Assertions.assertTrue(this[nøkkel]!!)
        this.filterKeys { it != nøkkel }.values.forEach { value ->
            assertFalse(value)
        }
    }

    //language=JSON
    private fun behovMelding(behovNavn: String) =
        """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "identer":[{"id":"12345678910","type":"folkeregisterident","historisk":false}],
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "@behov": [
       "$behovNavn"
      ],
      "InnsendtSøknadsId":{"lastOppTidsstempel":"2020-11-26T10:33:38.684844","urn":"urn:soknadid:321"}
    }
        """.trimIndent()

    //language=JSON
    private fun meldingMedFlereBehov() =
        """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "@behovId": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "identer":[{"id":"12345678910","type":"folkeregisterident","historisk":false}],
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "@behov": [
       "JobbetUtenforNorge",
       "Verneplikt"
      ],
      "InnsendtSøknadsId":{"lastOppTidsstempel":"2020-11-26T10:33:38.684844","urn":"urn:soknadid:321"}
    }
        """.trimIndent()
}
