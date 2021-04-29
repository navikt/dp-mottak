package no.nav.dagpenger.mottak.behov.journalpost

class JournalpostApi {
    internal data class OppdaterJournalpostRequest(
        val bruker: Bruker,
        val tittel: String,
        val sak: Sak,
        val dokumenter: List<Dokument>,
        val behandlingstema: String = "ab0001",
        val tema: String = "DAG",
        val journalfoerendeEnhet: String = "9999"
    )

    internal data class Sak(val fagsakId: String?) {
        val saksType: SaksType
        val fagsaksystem: String?

        init {
            if (fagsakId != null) {
                saksType = SaksType.FAGSAK
                fagsaksystem = "AO01"
            } else {
                saksType = SaksType.GENERELL_SAK
                fagsaksystem = null
            }
        }
    }

    internal data class Bruker(val id: String, val idType: String = "FNR")
    internal data class Dokument(val dokumentInfoId: String, val tittel: String)
    internal enum class SaksType {
        GENERELL_SAK,
        FAGSAK
    }
}