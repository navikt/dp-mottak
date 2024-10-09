package no.nav.dagpenger.mottak.behov.journalpost

import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.assertTrue

internal class JournalpostBehovLøserTest {
    private companion object {
        val JOURNALPOST_ID = "124567"

        //language=JSON
        val journalpostJson = """{
              "data": {
                "journalpost": {
                  "journalpostId": "$JOURNALPOST_ID",
                  "bruker": {
                    "id": "12345678901",
                    "type": "FNR"
                  },
                  "relevanteDatoer": [
                    {
                      "dato": "${LocalDateTime.now()}",
                      "datotype": "DATO_REGISTRERT"
                    }
                  ],
                  "dokumenter": [
                    {
                      "tittel": null,
                      "dokumentInfoId": 1234,
                      "brevkode": "NAV 04-01.03"
                    },
                    {
                      "tittel": null,
                      "dokumentInfoId": 5678,
                      "brevkode": "N6"
                    }
                  ],
                  "behandlingstema": null
                }
              }
            }"""
    }

    private val testRapid = TestRapid()

    init {
        JournalpostBehovLøser(
            rapidsConnection = testRapid,
            journalpostArkiv =
                object : JournalpostArkiv {
                    override suspend fun hentJournalpost(journalpostId: String): SafGraphQL.Journalpost =
                        SafGraphQL.Journalpost.fromGraphQlJson(journalpostJson)
                },
        )
    }

    @Test
    fun `Skal hente saf post og legge på kafka `() {
        testRapid.sendTestMessage(journalpostBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertEquals("124567", field(0, "@løsning")["Journalpost"]["journalpostId"].asText())
            assertEquals("NAV 04-01.03", field(0, "@løsning")["Journalpost"]["dokumenter"][0]["brevkode"].asText())
            assertTrue(field(0, "@løsning")["Journalpost"]["dokumenter"][0]["hovedDokument"].asBoolean())
            assertEquals("N6", field(0, "@løsning")["Journalpost"]["dokumenter"][1]["brevkode"].asText())
            assertFalse(field(0, "@løsning")["Journalpost"]["dokumenter"][1]["hovedDokument"].asBoolean())
            assertDoesNotThrow { field(0, "@løsning")["Journalpost"]["relevanteDatoer"][0]["dato"].asLocalDateTime() }
        }
    }

    @Test
    fun `Kaster excpetion om error liste ikke er tom`() {
        assertThrows<Throwable> {
            SafGraphQL.Journalpost.fromGraphQlJson("""{"errors": ["Her er en error"],"data":null}""".trimIndent())
        }
    }

    private fun journalpostBehov(): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "Journalpost"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "$JOURNALPOST_ID"
        }
        """.trimIndent()
}
