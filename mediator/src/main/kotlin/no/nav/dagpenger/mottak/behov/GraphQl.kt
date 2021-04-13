package no.nav.dagpenger.mottak.behov

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal open class GraphqlQuery(val query: String, val variables: Any?) {
    companion object {
        internal val jacksonJsonAdapter = jacksonObjectMapper().also {
            it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }
    fun toJson(): String = jacksonJsonAdapter.writeValueAsString(this)
}
