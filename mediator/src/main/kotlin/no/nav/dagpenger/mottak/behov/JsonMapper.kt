package no.nav.dagpenger.mottak.behov

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder

object JsonMapper {
    internal val jacksonJsonAdapter =
        jacksonMapperBuilder()
            .addModule(JavaTimeModule())
            .build()
            .also {
                it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }
}
