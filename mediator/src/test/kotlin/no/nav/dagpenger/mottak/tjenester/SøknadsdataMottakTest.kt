package no.nav.dagpenger.mottak.tjenester

import io.mockk.mockk
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test

internal class SøknadsdataMottakTest {
    private val rapid = TestRapid()
    private val service = SøknadsdataMottak(mockk(), rapid)

    @Test
    fun lol() {
        rapid.sendTestMessage(søknadsdataJSON.toJson())
    }
}

private val søknadsdataJSON = JsonMessage.newNeed(
    listOf("Søknadsdata"),
    mapOf(
        "journalpostId" to "123",
        "@løsning" to mapOf("Søknadsdata" to "")
    )
)
