package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Hendelse

class JournalpostOppdatert(private val journalpostId: String) : Hendelse() {
    override fun journalpostId(): String = journalpostId
}
