package no.nav.dagpenger.mottak

import java.time.LocalDateTime

data class InnsendingPeriode(val ident: String, val registrertDato: LocalDateTime, val journalpostId: String)
