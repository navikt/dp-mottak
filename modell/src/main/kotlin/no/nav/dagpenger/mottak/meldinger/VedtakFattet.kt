package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetskontekst
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.IAktivitetslogg
import no.nav.dagpenger.mottak.SpesifikkKontekst
import java.util.UUID

class VedtakFattet(
    val fagsakId: String,
    val fagsystem: String,
    val søknadId: UUID,
    val behandlingId: UUID,
    val ident: String,
    internal val aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
) : IAktivitetslogg by aktivitetslogg, Aktivitetskontekst {
    init {
        aktivitetslogg.kontekst(this)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst {
        return this.javaClass.canonicalName.split('.').last().let {
            SpesifikkKontekst(it, mapOf())
        }
    }

    fun toLogString() = aktivitetslogg.toString()
}
