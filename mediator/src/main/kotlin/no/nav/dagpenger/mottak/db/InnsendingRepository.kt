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

    fun hentJournalpostIder(
        søknadId: UUID,
        ident: String,
    ): List<String>

    fun opprettKoblingTilNyJournalpostForSak(
        nyJournalpostId: Int,
        innsendingId: Int,
        sakId: UUID,
    )

    fun finnJournalpostIderForSøknad(ident: String, søknadId: UUID): List<String>
}

data class ArenaOppgave(
    val journalpostId: String,
    val oppgaveId: String,
    val fagsakId: String?,
    val innsendingId: Int,
)
