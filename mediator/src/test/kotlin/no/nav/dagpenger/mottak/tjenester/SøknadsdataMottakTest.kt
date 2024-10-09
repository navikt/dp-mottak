package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

internal class SøknadsdataMottakTest {
    private val rapid = TestRapid()
    private val service = SøknadsdataMottak(mockk(), rapid)

    @Test
    @Disabled
    fun lol() {
        rapid.sendTestMessage(søknadsdataJSON.toJson())
    }
}

private val søknadsdataJSON =
    JsonMessage.newNeed(
        listOf("Søknadsdata"),
        mapOf(
            "journalpostId" to "123",
            "@løsning" to mapOf("Søknadsdata" to ""),
        ),
    )
