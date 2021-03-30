package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Hendelse

class JournalpostFerdigstilt(private val journalpostId: String) : Hendelse() {
    override fun journalpostId(): String = journalpostId
}
