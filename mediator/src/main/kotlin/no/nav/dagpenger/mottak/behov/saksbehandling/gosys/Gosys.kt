package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import java.time.LocalDate

internal interface GosysOppslag {
    suspend fun opprettOppgave(oppgave: GosysOppgaveParametre): String
    // header("X-Correlation-ID", journalPostId)
}

internal data class GosysOppgaveParametre(
    val journalpostId: String,
    val aktørId: String?,
    val tildeltEnhetsnr: String,
    val aktivDato: LocalDate,
    val fristFerdigstillelse: LocalDate = aktivDato,
    val prioritet: String = "NORM",
    val beskrivelse: String = "Kunne ikke automatisk journalføres",
) {
    val oppgavetype: String = "JFR"
    val opprettetAvEnhetsnr: String = "9999"
    val tema: String = "DAG"
}
