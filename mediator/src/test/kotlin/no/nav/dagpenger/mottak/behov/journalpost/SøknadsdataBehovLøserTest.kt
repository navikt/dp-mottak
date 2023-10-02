package no.nav.dagpenger.mottak.behov.journalpost

import no.nav.dagpenger.mottak.behov.JsonMapper.jacksonJsonAdapter
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class SøknadsdataBehovLøserTest {
    private val testRapid = TestRapid()

    init {
        SøknadsdataBehovLøser(
            rapidsConnection = testRapid,
            søknadsArkiv =
                object : SøknadsArkiv {
                    override suspend fun hentSøknadsData(
                        journalpostId: String,
                        dokumentInfoId: String,
                    ): SafGraphQL.SøknadsData {
                        return SafGraphQL.SøknadsData(
                            jacksonJsonAdapter.createObjectNode().apply {
                                put("something", "something")
                                put("something else", "something else")
                                put("something entirely different", "something entirely different")
                            },
                        )
                    }
                },
        )
    }

    @Test
    fun `løser søknadsdata behov`() {
        testRapid.sendTestMessage(journalpostBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertNotNull(field(0, "@løsning")["Søknadsdata"])
        }
    }

    //language=JSON
    private fun journalpostBehov(): String =
        """
        {
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "Søknadsdata"
          ],
          "dokumentInfoId":"526471",
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "123456"
        }
        """.trimIndent()
}
