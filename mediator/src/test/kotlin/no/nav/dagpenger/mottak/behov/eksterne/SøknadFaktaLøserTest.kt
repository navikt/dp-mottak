package no.nav.dagpenger.mottak.behov.eksterne

import io.mockk.every
import io.mockk.mockkClass
import no.nav.dagpenger.mottak.behov.eksterne.AvsluttetArbeidsforhold.Sluttårsak
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.time.LocalDate

internal class SøknadFaktaLøserTest {

    val testRapid = TestRapid()
    val testSøknad = mockkClass(Søknad::class).apply {
        every { ønskerDagpengerFraDato() } returns LocalDate.parse("2021-05-25")
        every { søknadstidspunkt() } returns LocalDate.parse("2021-05-23")
        every { verneplikt() } returns false
        every { fangstOgFisk() } returns false
        every { sisteDagMedArbeidsplikt() } returns LocalDate.parse("2021-05-24")
        every { sisteDagMedLønn() } returns LocalDate.parse("2021-06-15")
        every { lærling() } returns true
        every { jobbetIeøs() } returns false
        every { rettighetstype() } returns Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER
    }

    init {
        SøknadFaktaLøser(
            søknadsOppslag = object : SøknadsOppslag {
                override fun hentSøknad(journalpostId: String): Søknad = testSøknad
            },
            rapidsConnection = testRapid
        )
    }

    @BeforeEach
    fun setup() {
        testRapid.reset()
    }

    @ParameterizedTest
    @CsvSource(
        value = [
            "ØnskerDagpengerFraDato:2021-05-25",
            "Søknadstidspunkt:2021-05-23",
            "Verneplikt:false",
            "FangstOgFiske:false",
            "SisteDagMedArbeidsplikt:2021-05-24",
            "SisteDagMedLønn:2021-06-15",
            "Lærling:true",
            "EØSArbeid:false",
            "Rettighetstype:SAGT_OPP_AV_ARBEIDSGIVER"
        ],
        delimiter = ':'
    )
    fun `besvarer fakta behov`(behovNavn: String, forventetVerdi: String) {
        testRapid.sendTestMessage(behovMelding(behovNavn))
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertEquals(forventetVerdi, field(0, "@løsning")[behovNavn].asText())
        }
    }

    //language=JSON
    private fun behovMelding(behovNavn: String) =
        """
    {
      "@event_name": "faktum_svar",
      "@opprettet": "2020-11-18T11:04:32.867824",
      "@id": "930e2beb-d394-4024-b713-dbeb6ad3d4bf",
      "identer":[{"id":"12345678910","type":"folkeregisterident","historisk":false}],
      "søknad_uuid": "41621ac0-f5ee-4cce-b1f5-88a79f25f1a5",
      "@behov": [
       "$behovNavn"
      ],
      "InnsendtSøknadsId":{"lastOppTidsstempel":"2020-11-26T10:33:38.684844","url":"321"}
    }
        """.trimIndent()
}
