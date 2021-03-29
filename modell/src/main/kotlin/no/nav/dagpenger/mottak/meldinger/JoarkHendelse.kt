package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Hendelse

class JoarkHendelse(
    private val journalpostId: String,
    private val hendelseType: String,
    private val journalpostStatus: String,
    private val behandlingstema: String? = null
) : Hendelse() {

    override fun journalpostId() = journalpostId
}
