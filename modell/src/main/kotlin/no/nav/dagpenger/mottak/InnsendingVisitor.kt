package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.meldinger.Journalpost
import java.time.LocalDateTime

interface JournalpostVisitor {
    fun visitJournalpost(
        journalpostId: String,
        journalpostStatus: String,
        bruker: Journalpost.Bruker?,
        behandlingstema: String?,
        registrertDato: LocalDateTime,
        dokumenter: List<Journalpost.DokumentInfo>
    ) {
    }
}

interface SøknadVisitor {
    fun visitSøknad(søknad: SøknadOppslag?) {}
}

interface PersonVisitor {
    fun visitPerson(navn: String, aktørId: String, ident: String, norskTilknytning: Boolean, diskresjonskode: Boolean) {}
}

interface ArenaSakVisitor {
    fun visitArenaSak(oppgaveId: String, fagsakId: String?) {}
}

interface InnsendingVisitor :
    JournalpostVisitor,
    SøknadVisitor,
    PersonVisitor,
    ArenaSakVisitor,
    AktivitetsloggVisitor {
    fun preVisitInnsending(innsending: Innsending, journalpostId: String) {}
    fun visitTilstand(tilstandType: Innsending.Tilstand) {}
    fun visitInnsending(oppfyllerMinsteArbeidsinntekt: Boolean?, eksisterendeSaker: Boolean?) {}
    fun visitInnsendingAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun postVisitInnsending(innsending: Innsending, journalpostId: String) {}
}
