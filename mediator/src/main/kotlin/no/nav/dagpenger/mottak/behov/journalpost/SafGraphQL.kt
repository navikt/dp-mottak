package no.nav.dagpenger.mottak.behov.journalpost

import no.nav.dagpenger.mottak.defaultObjectMapper
import tools.jackson.core.JsonParser
import tools.jackson.databind.DeserializationContext
import tools.jackson.databind.JsonNode
import tools.jackson.databind.ValueDeserializer
import tools.jackson.databind.annotation.JsonDeserialize

internal class SafGraphQL {
    data class Bruker(
        val type: BrukerType,
        val id: String,
    ) {
        override fun toString(): String {
            return "Bruker(type=$type, id='<REDACTED>')"
        }
    }

    enum class BrukerType {
        ORGNR,
        AKTOERID,
        FNR,
    }

    data class RelevantDato(
        val dato: String,
        val datotype: Datotype,
    )

    class DokumentInfo(val tittel: String?, val dokumentInfoId: String, val brevkode: String?, val hovedDokument: Boolean) {
        override fun toString(): String {
            return "DokumentInfo(tittel=$tittel, dokumentInfoId=$dokumentInfoId, brevkode=$brevkode)"
        }
    }

    internal data class Journalpost(
        val journalstatus: Journalstatus?,
        val journalpostId: String,
        val bruker: Bruker?,
        val tittel: String?,
        val datoOpprettet: String?,
        val journalfoerendeEnhet: String?,
        val relevanteDatoer: List<RelevantDato>,
        @JsonDeserialize(using = DokumentInfoDeserializer::class)
        val dokumenter: List<DokumentInfo>,
        val behandlingstema: String? = null,
    ) {
        internal companion object {
            private val objectMapper = defaultObjectMapper

            fun fromGraphQlJson(json: String): Journalpost =
                objectMapper.readValue(json, GraphQlJournalpostResponse::class.java).data?.journalpost
                    ?: throw IllegalArgumentException("SAF response har ingen data")
        }

        private data class GraphQlJournalpostResponse(val data: Data?, val errors: List<String>?) {
            init {
                if (errors?.isNotEmpty() == true) {
                    throw IllegalArgumentException("SAF returnerte liste med feil: ${errors.joinToString("\n")}")
                }
            }

            class Data(val journalpost: Journalpost)
        }
    }

    enum class Datotype {
        DATO_SENDT_PRINT,
        DATO_EKSPEDERT,
        DATO_JOURNALFOERT,
        DATO_REGISTRERT,
        DATO_AVS_RETUR,
        DATO_DOKUMENT,
        DATO_LEST,
    }

    enum class Journalstatus {
        MOTTATT,
        JOURNALFOERT,
        FERDIGSTILT,
        EKSPEDERT,
        UNDER_ARBEID,
        FEILREGISTRERT,
        UTGAAR,
        AVBRUTT,
        UKJENT_BRUKER,
        RESERVERT,
        OPPLASTING_DOKUMENT,
        UKJENT,
    }
}

internal class DokumentInfoDeserializer : ValueDeserializer<List<SafGraphQL.DokumentInfo>>() {
    override fun deserialize(
        p: JsonParser,
        ctxt: DeserializationContext,
    ): List<SafGraphQL.DokumentInfo> {
        val node: JsonNode = p.readValueAsTree()
        return node.values().mapIndexed { index, dokument ->
            SafGraphQL.DokumentInfo(
                tittel = dokument["tittel"].stringValue(),
                brevkode = dokument["brevkode"].stringValue(),
                dokumentInfoId = dokument["dokumentInfoId"].asString(),
                hovedDokument = index == 0,
            )
        }
    }
}
