package no.nav.dagpenger.mottak

import tools.jackson.databind.JsonNode
import java.io.FileNotFoundException

internal val jackson = defaultObjectMapper

internal fun String.toJsonNode(): JsonNode = jackson.readTree(this)

internal fun String.lesFil(): String {
    return object {}.javaClass.getResource(this)?.readText()
        ?: throw FileNotFoundException("Fant ikke $this på classpath")
}

internal fun String.jsonFraFil() = this.lesFil().toJsonNode()
