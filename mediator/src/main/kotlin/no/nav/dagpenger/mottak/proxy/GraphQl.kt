package no.nav.dagpenger.mottak.proxy

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal open class GraphqlQuery(val query: String, val variables: Any?) {
    private companion object {
        val jsonMapper = jacksonObjectMapper()
    }
    fun toJson(): String = jsonMapper.writeValueAsString(this)
}
