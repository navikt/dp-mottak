package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.toJsonNode
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class SøknadsdataTest {

    @Test
    fun `Lager riktig søknads format`() {
        søknadsData(
            """{"fakta": []}""".toJsonNode()
        ).also {
            assertTrue(it.søknad() is GammeltSøknadFormat)
        }

        søknadsData(
            """ {"versjon_navn": "Dagpenger", "seksjoner": []} """.toJsonNode()
        ).also {
            assertTrue(it.søknad() is QuizSøknadFormat)
        }

        søknadsData(""" {"hubba": []} """.toJsonNode()).søknad().also {
            assertTrue(it is GammeltSøknadFormat)
        }
    }

    private fun søknadsData(data: JsonNode) = Søknadsdata(
        aktivitetslogg = Aktivitetslogg(forelder = null),
        journalpostId = "joid",
        data = data
    )
}
