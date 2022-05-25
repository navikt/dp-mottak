package no.nav.dagpenger.mottak.meldinger

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.mottak.Aktivitetslogg
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.io.FileNotFoundException

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

private val jackson = jacksonObjectMapper()
private fun String.toJsonNode(): JsonNode = jackson.readTree(this)

private fun String.lesFil(): String {
    return object {}.javaClass.getResource(this)?.readText()
        ?: throw FileNotFoundException("Fant ikke $this på classpath")
}
