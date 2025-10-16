package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.meldinger.Journalpost
import java.time.LocalDateTime
import java.util.UUID

interface JournalpostVisitor {
    fun visitJournalpost(
        journalpostId: String,
        journalpostStatus: String,
        bruker: Journalpost.Bruker?,
        behandlingstema: String?,
        journalførendeEnhet: String?,
        registrertDato: LocalDateTime,
        dokumenter: List<Journalpost.DokumentInfo>,
    ) {
    }
}

interface SøknadVisitor {
    fun visitSøknad(søknad: SøknadOppslag?) {}
}

interface PersonVisitor {
    fun visitPerson(
        navn: String,
        aktørId: String,
        ident: String,
        norskTilknytning: Boolean,
        diskresjonskode: Boolean,
        egenAnsatt: Boolean,
    ) {
    }
}

interface ArenaSakVisitor {
    fun visitArenaSak(
        oppgaveId: String,
        fagsakId: String?,
    ) {}
}

interface OppgaveSakVisitor {
    fun visitOppgaveSak(
        oppgaveId: UUID?,
        fagsakId: UUID,
    ) {}
}

interface InnsendingVisitor :
    JournalpostVisitor,
    SøknadVisitor,
    PersonVisitor,
    ArenaSakVisitor,
    OppgaveSakVisitor,
    AktivitetsloggVisitor {
    fun preVisitInnsending(
        innsending: Innsending,
        journalpostId: String,
    ) {}

    fun visitTilstand(tilstandType: Innsending.Tilstand) {}

    fun visitInnsendingAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}

    fun postVisitInnsending(
        innsending: Innsending,
        journalpostId: String,
    ) {}
}
