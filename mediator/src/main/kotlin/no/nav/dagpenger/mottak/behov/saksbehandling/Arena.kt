package no.nav.dagpenger.mottak.behov.saksbehandling

import java.time.LocalDate

interface ArenaApiClient {
    suspend fun harEksisterendeSaker(fnr: String, virkningstidspunkt: LocalDate = LocalDate.now()): Boolean
}

data class EksisterendeSaker(val journalpostId: String, val harEksisterendeSaker: Boolean)
// api: arena/sak/aktiv
