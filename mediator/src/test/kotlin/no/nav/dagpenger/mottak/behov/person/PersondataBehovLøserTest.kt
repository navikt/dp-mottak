package no.nav.dagpenger.mottak.behov.person

import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class PersondataBehovLøserTest {
    private companion object {
        val JOURNALPOST_ID = "124567"
    }

    private val testRapid = TestRapid()

    init {
        PersondataBehovLøser(
            rapidsConnection = testRapid,
            personOppslag = object : PersonOppslag {
                override suspend fun hentPerson(id: String): Pdl.Person = Pdl.Person(
                    navn = "Hubba bubba",
                    aktørId = "12345678",
                    fødselsnummer = "1234567891",
                    norskTilknytning = true,
                    diskresjonskode = "diskresjonskode"
                )
            }
        )
    }

    @Test
    fun `Skal løse persondata behov`() {
        testRapid.sendTestMessage(persondataBehov())
        with(testRapid.inspektør) {
            Assertions.assertEquals(1, size)
            Assertions.assertEquals("12345678", field(0, "@løsning")["Persondata"]["aktørId"].asText())
            Assertions.assertEquals("1234567891", field(0, "@løsning")["Persondata"]["fødselsnummer"].asText())
            Assertions.assertEquals("true", field(0, "@løsning")["Persondata"]["norskTilknytning"].asText())
            Assertions.assertEquals("diskresjonskode", field(0, "@løsning")["Persondata"]["diskresjonskode"].asText())
        }
    }

    private fun persondataBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Persondata"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "brukerId": "87654321"
        }
        """.trimIndent()
}
