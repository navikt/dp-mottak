package no.nav.dagpenger.mottak

import java.time.LocalDate

data class InnsendingPeriode(val ident: String, val registrertDato: LocalDate, val journalpostId: String)
