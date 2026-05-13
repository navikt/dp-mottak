package no.nav.dagpenger.mottak.behov

import no.nav.dagpenger.mottak.defaultObjectMapper

internal open class GraphqlQuery(val query: String, val variables: Any?) {
    fun toJson(): String = defaultObjectMapper.writeValueAsString(this)
}
