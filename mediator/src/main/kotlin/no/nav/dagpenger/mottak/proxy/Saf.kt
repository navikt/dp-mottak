package no.nav.dagpenger.mottak.proxy

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
        val journalforendeEnhet: String?,
        val relevanteDatoer: List<RelevantDato>,
        val dokumenter: List<DokumentInfo>,
        val behandlingstema: String? = null
    ) {

        internal companion object {
            private val objectMapper = jacksonObjectMapper()
            fun fromJson(json: String): Journalpost = objectMapper.readValue<Journalpost>(json, Journalpost::class.java)
        }
    }

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
