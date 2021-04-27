package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import java.time.ZonedDateTime

interface JournalpostVisitor {
    fun visitJournalpost(
        journalpostId: String,
        journalpostStatus: String,
        bruker: Journalpost.Bruker?,
        behandlingstema: String?,
        registrertDato: ZonedDateTime,
        dokumenter: List<Journalpost.DokumentInfo>
    ) {
    }
}

interface SøknadVisitor {
    fun visitSøknad(søknad: Søknadsdata.Søknad?) {}
}

interface PersonVisitor {
    fun visitPerson(aktørId: String, fødselsnummer: String, norskTilknytning: Boolean, diskresjonskode: Boolean) {}
}

interface ArenaSakVisitor {
    fun visitArenaSak(oppgaveId: String, fagsakId: String) {}
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
