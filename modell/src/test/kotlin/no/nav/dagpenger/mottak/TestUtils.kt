package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import java.io.FileNotFoundException

internal val jackson = jacksonObjectMapper()

internal fun String.toJsonNode(): JsonNode = jackson.readTree(this)

internal fun String.lesFil(): String {
    return object {}.javaClass.getResource(this)?.readText()
        ?: throw FileNotFoundException("Fant ikke $this på classpath")
}

internal fun String.jsonFraFil() = this.lesFil().toJsonNode()
