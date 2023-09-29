package no.nav.dagpenger.mottak.serder

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.SpesifikkKontekst
import no.nav.dagpenger.mottak.serder.ReflectInstance.Companion.get

internal fun konverterTilAktivitetslogg(aktivitetsloggData: InnsendingData.AktivitetsloggData): Aktivitetslogg {
    val aktivitetslogg = Aktivitetslogg()

    val aktiviteter = aktivitetslogg.get<MutableList<Any>>("aktiviteter")
    aktivitetsloggData.aktiviteter.forEach {
        val kontekster =
            it.kontekster.map { spesifikkKontekstData ->
                SpesifikkKontekst(
                    spesifikkKontekstData.kontekstType,
                    spesifikkKontekstData.kontekstMap,
                )
            }
        aktiviteter.add(
            when (it.alvorlighetsgrad) {
                InnsendingData.AktivitetsloggData.Alvorlighetsgrad.INFO ->
                    Aktivitetslogg.Aktivitet.Info(
                        kontekster,
                        it.melding,
                        it.tidsstempel,
                    )
                InnsendingData.AktivitetsloggData.Alvorlighetsgrad.WARN ->
                    Aktivitetslogg.Aktivitet.Warn(
                        kontekster,
                        it.melding,
                        it.tidsstempel,
                    )
                InnsendingData.AktivitetsloggData.Alvorlighetsgrad.BEHOV ->
                    Aktivitetslogg.Aktivitet.Behov(
                        Aktivitetslogg.Aktivitet.Behov.Behovtype.valueOf(it.behovtype!!),
                        kontekster,
                        it.melding,
                        it.detaljer,
                        it.tidsstempel,
                    )
                InnsendingData.AktivitetsloggData.Alvorlighetsgrad.ERROR ->
                    Aktivitetslogg.Aktivitet.Error(
                        kontekster,
                        it.melding,
                        it.tidsstempel,
                    )
                InnsendingData.AktivitetsloggData.Alvorlighetsgrad.SEVERE ->
                    Aktivitetslogg.Aktivitet.Severe(
                        kontekster,
                        it.melding,
                        it.tidsstempel,
                    )
            },
        )
    }

    return aktivitetslogg
}
