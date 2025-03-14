package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingPeriode
import no.nav.dagpenger.mottak.api.Periode
import java.util.UUID

interface InnsendingRepository {
    fun hent(journalpostId: String): Innsending?

    fun lagre(innsending: Innsending): Int

    fun forPeriode(periode: Periode): List<InnsendingPeriode>
}

interface InnsendingMetadataRepository {
    fun hentArenaOppgaver(
        søknadId: UUID,
        ident: String,
    ): List<ArenaOppgave>
}

data class ArenaOppgave(
    val journalpostId: Int,
    val oppgaveId: String,
    val fagsakId: String?,
)
