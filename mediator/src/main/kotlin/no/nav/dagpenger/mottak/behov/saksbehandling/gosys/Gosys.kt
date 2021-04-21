package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import java.time.LocalDate

internal interface GosysOppslag{
    suspend fun opprettOppgave(oppgave: GosysOppgave)
}


internal data class GosysOppgave(
    val journalpostId: String,
    val aktørId: String?,
    val tildeltEnhetsnr: String,
    val beskrivelse: String = "Kunne ikke automatisk journalføres",
    val opprettetAvEnhetsnr: String = "9999",
    val tema: String = "DAG",
    val oppgavetype: String = "JFR",
    val aktivDato: LocalDate,
    val fristFerdigstillelse: LocalDate,
    val prioritet: String = "NORM"
)