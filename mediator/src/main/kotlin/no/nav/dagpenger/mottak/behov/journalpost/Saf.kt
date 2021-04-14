package no.nav.dagpenger.mottak.behov.journalpost

import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper

internal class Saf {
    data class Bruker(
        val type: BrukerType,
        val id: String
    ) {
        override fun toString(): String {
            return "Bruker(type=$type, id='<REDACTED>')"
        }
    }

    enum class BrukerType {
        ORGNR, AKTOERID, FNR
    }

    data class RelevantDato(
        val dato: String,
        val datotype: Datotype
    )

    class DokumentInfo(tittel: String?, dokumentInfoId: String, brevkode: String?) {
        val tittel = tittel
        val dokumentInfoId = dokumentInfoId
        val brevkode = brevkode

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
        val dokumenter: List<DokumentInfo>,
        val behandlingstema: String? = null
    ) {

        internal companion object {
            private val objectMapper = jacksonObjectMapper().also {
                it.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
            }

            fun fromGraphQlJson(json: String): Journalpost =
                objectMapper.readValue(json, GraphQlJournalpostResponse::class.java).data?.journalpost ?: throw IllegalArgumentException("SAF response har ingen data")
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

    internal data class SøknadsData(val data: JsonNode)

    enum class Datotype {
        DATO_SENDT_PRINT, DATO_EKSPEDERT, DATO_JOURNALFOERT,
        DATO_REGISTRERT, DATO_AVS_RETUR, DATO_DOKUMENT
    }

    enum class Journalstatus {
        MOTTATT, JOURNALFOERT, FERDIGSTILT, EKSPEDERT,
        UNDER_ARBEID, FEILREGISTRERT, UTGAAR, AVBRUTT,
        UKJENT_BRUKER, RESERVERT, OPPLASTING_DOKUMENT,
        UKJENT
    }
}
