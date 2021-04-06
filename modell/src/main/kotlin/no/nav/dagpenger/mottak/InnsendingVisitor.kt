package no.nav.dagpenger.mottak

internal interface InnsendingVisitor : AktivitetsloggVisitor {
    fun preVisitInnsending(innsending: Innsending, journalpostId: String) {}
    fun visitTilstand(tilstandType: Innsending.Tilstand) {}
    fun visitInnsendingAktivitetslogg(aktivitetslogg: Aktivitetslogg) {}
    fun postVisitInnsending(innsending: Innsending, journalpostId: String) {}
}
