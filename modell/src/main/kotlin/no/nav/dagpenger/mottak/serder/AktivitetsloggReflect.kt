package no.nav.dagpenger.mottak.serder

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Info
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Warn
import no.nav.dagpenger.mottak.AktivitetsloggVisitor
import no.nav.dagpenger.mottak.SpesifikkKontekst
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData.Alvorlighetsgrad
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData.Alvorlighetsgrad.BEHOV
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData.Alvorlighetsgrad.ERROR
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData.Alvorlighetsgrad.INFO
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData.Alvorlighetsgrad.SEVERE
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData.Alvorlighetsgrad.WARN

internal class AktivitetsloggReflect(aktivitetslogg: Aktivitetslogg) {
    private val aktiviteter = Aktivitetslogginspektør(aktivitetslogg).aktiviteter

    internal fun toMap() = mutableMapOf(
        "aktiviteter" to aktiviteter
    )

    private inner class Aktivitetslogginspektør(aktivitetslogg: Aktivitetslogg) : AktivitetsloggVisitor {
        internal val aktiviteter = mutableListOf<Map<String, Any>>()

        init {
            aktivitetslogg.accept(this)
        }

        override fun visitInfo(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Info,
            melding: String,
            tidsstempel: String
        ) {
            leggTilMelding(kontekster, INFO, melding, tidsstempel)
        }

        override fun visitWarn(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Warn,
            melding: String,
            tidsstempel: String
        ) {
            leggTilMelding(kontekster, WARN, melding, tidsstempel)
        }

        override fun visitBehov(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Aktivitetslogg.Aktivitet.Behov,
            type: Behovtype,
            melding: String,
            detaljer: Map<String, Any>,
            tidsstempel: String
        ) {
            leggTilBehov(
                kontekster,
                BEHOV,
                type,
                melding,
                detaljer,
                tidsstempel
            )
        }

        override fun visitError(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Aktivitetslogg.Aktivitet.Error,
            melding: String,
            tidsstempel: String
        ) {
            leggTilMelding(kontekster, ERROR, melding, tidsstempel)
        }

        override fun visitSevere(
            kontekster: List<SpesifikkKontekst>,
            aktivitet: Aktivitetslogg.Aktivitet.Severe,
            melding: String,
            tidsstempel: String
        ) {
            leggTilMelding(kontekster, SEVERE, melding, tidsstempel)
        }

        private fun leggTilMelding(
            kontekster: List<SpesifikkKontekst>,
            alvorlighetsgrad: Alvorlighetsgrad,
            melding: String,
            tidsstempel: String
        ) {
            aktiviteter.add(
                mutableMapOf<String, Any>(
                    "kontekster" to map(kontekster),
                    "alvorlighetsgrad" to alvorlighetsgrad.name,
                    "melding" to melding,
                    "detaljer" to emptyMap<String, Any>(),
                    "tidsstempel" to tidsstempel
                )
            )
        }

        private fun leggTilBehov(
            kontekster: List<SpesifikkKontekst>,
            alvorlighetsgrad: Alvorlighetsgrad,
            type: Behovtype,
            melding: String,
            detaljer: Map<String, Any>,
            tidsstempel: String
        ) {
            aktiviteter.add(
                mutableMapOf<String, Any>(
                    "kontekster" to map(kontekster),
                    "alvorlighetsgrad" to alvorlighetsgrad.name,
                    "behovtype" to type.toString(),
                    "melding" to melding,
                    "detaljer" to detaljer,
                    "tidsstempel" to tidsstempel
                )
            )
        }

        private fun map(kontekster: List<SpesifikkKontekst>): List<Map<String, Any>> {
            return kontekster.map {
                mutableMapOf(
                    "kontekstType" to it.kontekstType,
                    "kontekstMap" to it.kontekstMap
                )
            }
        }
    }
}
