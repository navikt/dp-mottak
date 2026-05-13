package no.nav.dagpenger.mottak

import tools.jackson.databind.JsonNode

internal object JsonMessageExtensions {
    fun JsonNode.getOrNull(key: String): JsonNode? = if (this[key].isNull) null else this[key]
}
