package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder

internal object JsonMapper {
    internal val jacksonJsonAdapter =
        jacksonMapperBuilder()
            .addModule(JavaTimeModule())
            .build()
            .also {
                it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                it.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
            }
}
