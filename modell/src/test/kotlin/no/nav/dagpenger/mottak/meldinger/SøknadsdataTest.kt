package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.toJsonNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

internal class SøknadsdataTest {

    @Test
    fun `Lager riktig søknads format`() {
        søknadsData(
            """{"fakta": []}""".toJsonNode()
        ).also {
            assertTrue(it.søknad() is GammeltSøknadFormat)
        }

        søknadsData(
            """ {"seksjoner": []} """.toJsonNode()
        ).also {
            assertTrue(it.søknad() is NyttSøknadFormat)
        }

        assertThrows<IllegalArgumentException> {
            søknadsData(""" {"hubba": []} """.toJsonNode()).søknad()
        }
    }

    private fun søknadsData(data: JsonNode) = Søknadsdata(
        aktivitetslogg = Aktivitetslogg(forelder = null),
        journalpostId = "joid",
        data = data
    )
}
