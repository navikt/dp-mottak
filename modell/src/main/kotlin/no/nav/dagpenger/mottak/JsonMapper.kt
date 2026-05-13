package no.nav.dagpenger.mottak

import tools.jackson.databind.ObjectMapper
import tools.jackson.databind.introspect.DefaultAccessorNamingStrategy
import tools.jackson.module.kotlin.jacksonMapperBuilder

val defaultObjectMapper: ObjectMapper =
    jacksonMapperBuilder()
        .accessorNaming(
            DefaultAccessorNamingStrategy.Provider()
                .withFirstCharAcceptance(true, true),
        )
        .build()
