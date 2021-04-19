package no.nav.dagpenger.mottak.behov.saksbehandling

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.time.LocalDateTime
import java.util.UUID

private const val JOURNALPOST_ID = "2345678"
private const val FNR = "12345678910"
private val ID = UUID.randomUUID()

internal class EksisterendeSakerLøserTest{

    val testRapid = TestRapid()
    init {
        EksisterendeSakerLøser(testRapid)
    }

    @Test
    fun `Løser eksisterende saker behov`(){
        testRapid.sendTestMessage(eksisterendeSakerBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertEquals("HarHattDagpengerSiste36Mnd",field(0,"@behov").first().asText())
            assertEquals(ID, field(0,"@id").asText())
            assertDoesNotThrow { field(0,"Virkningstidpunkt") }
            assertEquals(JOURNALPOST_ID,field(0,"søknad_uuid").asText())
            assertEquals("folkeregisterident",field(0,"identer").first()["type"].asText())
            assertEquals(FNR,field(0,"identer").first()["id"].asText())
            assertFalse(field(0,"identer").first()["historisk"].asBoolean())
        }
    }


    //TODO: legge til aktørId i eksisterendeSaker melding
    //language=JSON
    private fun eksisterendeSakerBehov(): String =
        """{
          "@event_name": "behov",
          "@id": $ID,
          "@behov": [
            "EksisterendeSaker"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "fnr": "12345678910"
        }
        """.trimIndent()

    //language=JSON
    private fun eksisterendeSakerLøsningMelding(): String = """
        {
          "@id": $ID,
          "Virkningstispunkt": ${LocalDate.now()},
          "identer":[{"id":"$FNR","type":"folkeregisterident","historisk":false}],
          "søknad_uuid": $JOURNALPOST_ID,
          "@løsning": {
          "HarHattDagpengerSiste36Mnd": false
          }
        }
    """.trimIndent()

}