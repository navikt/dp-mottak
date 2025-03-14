package no.nav.dagpenger.mottak.behov.journalpost

internal interface JournalpostDokarkiv {
    suspend fun oppdaterJournalpost(
        journalpostId: String,
        journalpost: JournalpostApi.OppdaterJournalpostRequest,
        eksternReferanseId: String,
    )

    suspend fun ferdigstill(
        journalpostId: String,
        eksternReferanseId: String,
    )

    suspend fun knyttJounalPostTilNySak(
        journalpostId: Int,
        fagsakId: String,
        ident: String,
    )
}

internal class JournalpostApi {
    internal data class OppdaterJournalpostRequest(
        val avsenderMottaker: Avsender? = null,
        val bruker: Bruker,
        val tittel: String,
        val sak: Sak,
        val dokumenter: List<Dokument>,
        val behandlingstema: String = "ab0001",
        val tema: String = "DAG",
        val journalfoerendeEnhet: String = "9999",
    )

    internal data class Sak(
        val fagsakId: String?,
    ) {
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

    internal data class Avsender(
        val id: String,
        val idType: String = "FNR",
    )

    internal data class Bruker(
        val id: String,
        val idType: String = "FNR",
    )

    internal data class Dokument(
        val dokumentInfoId: String,
        val tittel: String,
    )

    internal enum class SaksType {
        GENERELL_SAK,
        FAGSAK,
    }
}
