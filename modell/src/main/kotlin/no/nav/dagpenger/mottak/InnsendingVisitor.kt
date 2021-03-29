package no.nav.dagpenger.mottak

interface InnsendingVisitor {
    fun preVisitInnsending(innsending: Innsending, journalpostId: String) {}
    fun visitTilstand(tilstandType: Innsending.Tilstand) {}
    fun postVisitInnsending(innsending: Innsending, journalpostId: String) {}
}
