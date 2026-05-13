package no.nav.dagpenger.mottak.serder

import tools.jackson.databind.JsonNode
import java.util.UUID

internal fun JsonNode.asUUID(): UUID = this.asString().let { UUID.fromString(it) }
