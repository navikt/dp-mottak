package no.nav.dagpenger.mottak.behov

import no.nav.dagpenger.mottak.behov.JsonMapper.jacksonJsonAdapter

internal open class GraphqlQuery(val query: String, val variables: Any?) {

    fun toJson(): String = jacksonJsonAdapter.writeValueAsString(this)
}
