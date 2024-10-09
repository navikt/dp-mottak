package no.nav.dagpenger.mottak.behov.person

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import no.nav.dagpenger.mottak.behov.person.PersonOppslag.Person
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class PersondataBehovLøserTest {
    private companion object {
        val JOURNALPOST_ID = "124567"
    }

    private val testRapid = TestRapid()

    @BeforeEach
    fun `clear rapid`() {
        testRapid.reset()
    }

    @Test
    fun `Skal løse persondata behov`() {
        PersondataBehovLøser(
            rapidsConnection = testRapid,
            personOppslag =
                object : PersonOppslag {
                    override suspend fun hentPerson(id: String): Person =
                        Person(
                            navn = "Hubba bubba",
                            aktørId = "12345678",
                            fødselsnummer = "1234567891",
                            norskTilknytning = true,
                            diskresjonskode = "diskresjonskode",
                            egenAnsatt = false,
                        )
                },
        )
        testRapid.sendTestMessage(persondataBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertEquals("12345678", field(0, "@løsning")["Persondata"]["aktørId"].asText())
            assertEquals("1234567891", field(0, "@løsning")["Persondata"]["fødselsnummer"].asText())
            assertEquals("true", field(0, "@løsning")["Persondata"]["norskTilknytning"].asText())
            assertEquals("diskresjonskode", field(0, "@løsning")["Persondata"]["diskresjonskode"].asText())
            assertEquals(false, field(0, "@løsning")["Persondata"]["egenAnsatt"].asBoolean())
        }
    }

    @Test
    fun `Skal løse persondata behov hvis person ikke er funnet`() {
        PersondataBehovLøser(
            rapidsConnection = testRapid,
            personOppslag =
                object : PersonOppslag {
                    override suspend fun hentPerson(id: String): Person? = null
                },
        )
        testRapid.sendTestMessage(persondataBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertTrue(field(0, "@løsning")["Persondata"].isNull)
        }
    }

    private fun persondataBehov(): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "Persondata"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID",
          "brukerId": "87654321"
        }
        """.trimIndent()
}
