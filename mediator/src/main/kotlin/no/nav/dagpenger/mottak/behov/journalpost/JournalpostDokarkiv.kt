package no.nav.dagpenger.mottak.behov.journalpost

import no.nav.dagpenger.mottak.Fagsystem

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
        journalpostId: String,
        dagpengerFagsakId: String,
        ident: String,
    ): KnyttJounalPostTilNySakResponse

    suspend fun feilregistrerSakstilknytning(
        journalpostId: String,
    )
}

internal data class KnyttJounalPostTilNySakResponse(
    val nyJournalpostId: String,
)

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
        val fagsystem: Fagsystem.FagsystemType?,
    ) {
        val saksType: SaksType
        val fagsaksystem: String?

        init {
            when (fagsakId) {
                null -> {
                    fagsaksystem = null
                    saksType = SaksType.GENERELL_SAK
                }
                else -> {
                    when (fagsystem) {
                        Fagsystem.FagsystemType.DAGPENGER -> {
                            saksType = SaksType.FAGSAK
                            fagsaksystem = "DAGPENGER"
                        }

                        Fagsystem.FagsystemType.ARENA -> {
                            saksType = SaksType.FAGSAK
                            fagsaksystem = "AO01"
                        }
                        else -> {
                            // skal ikke kunne se dette, men hvis det skjer så skal vi ikke kaste feil, men heller opprette en generell sak
                            saksType = SaksType.GENERELL_SAK
                            fagsaksystem = null
                        }
                    }
                }
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
