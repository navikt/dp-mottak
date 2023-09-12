package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse

class JoarkHendelse(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val hendelseType: String,
    private val journalpostStatus: String,
    private val behandlingstema: String? = null,
) : Hendelse(aktivitetslogg) {

    override fun journalpostId() = journalpostId
}
