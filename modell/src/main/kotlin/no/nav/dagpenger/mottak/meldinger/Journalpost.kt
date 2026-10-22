package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Anke
import no.nav.dagpenger.mottak.Etablering
import no.nav.dagpenger.mottak.Ettersending
import no.nav.dagpenger.mottak.Generell
import no.nav.dagpenger.mottak.Gjenopptak
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.JournalpostVisitor
import no.nav.dagpenger.mottak.KategorisertJournalpost
import no.nav.dagpenger.mottak.Klage
import no.nav.dagpenger.mottak.KlageForskudd
import no.nav.dagpenger.mottak.NySøknad
import no.nav.dagpenger.mottak.SpesifikkKontekst
import no.nav.dagpenger.mottak.UkjentSkjemaKode
import no.nav.dagpenger.mottak.Utdanning
import no.nav.dagpenger.mottak.UtenBruker
import no.nav.dagpenger.mottak.meldinger.Journalpost.DokumentInfo.Companion.hovedDokument
import no.nav.dagpenger.mottak.meldinger.Journalpost.DokumentInfo.Companion.vedlegg
import no.nav.dagpenger.mottak.meldinger.SkjemaType.ANKE
import no.nav.dagpenger.mottak.meldinger.SkjemaType.ANKE_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.Companion.tilSkjemaType
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_ORDINÆR
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_ORDINÆR_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_PERMITTERT
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_PERMITTERT_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.DAGPENGESØKNAD_UTDANNING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.GENERELL
import no.nav.dagpenger.mottak.meldinger.SkjemaType.GENERELL_FYLLUT
import no.nav.dagpenger.mottak.meldinger.SkjemaType.KLAGE
import no.nav.dagpenger.mottak.meldinger.SkjemaType.KLAGE_ETTERSENDING
import no.nav.dagpenger.mottak.meldinger.SkjemaType.KLAGE_OG_ANKE
import java.time.LocalDateTime
import java.time.temporal.ChronoUnit

class Journalpost constructor(
    aktivitetslogg: Aktivitetslogg = Aktivitetslogg(),
    private val journalpostId: String,
    private val journalpostStatus: String,
    private val journalførendeEnhet: String? = null,
    private val bruker: Bruker?,
    private val behandlingstema: String?,
    registrertDato: LocalDateTime,
    private val dokumenter: List<DokumentInfo>,
) : Hendelse(aktivitetslogg) {
    private val registrertDato: LocalDateTime = registrertDato.truncatedTo(ChronoUnit.MILLIS)

    override fun journalpostId(): String = journalpostId

    fun dokumenter() = dokumenter

    fun journalførendeEnhet() = journalførendeEnhet

    fun erEttersending() = hovedskjema().startsWith("NAVe")

    fun hovedskjema() = dokumenter.hovedDokument().brevkode

    fun hovedDokument() = dokumenter.hovedDokument()

    fun tittel() = dokumenter.hovedDokument().tittel

    fun bruker() = bruker

    fun status() = journalpostStatus

    fun vedlegg(): List<DokumentInfo> = dokumenter.vedlegg()

    fun datoRegistrert(): LocalDateTime = registrertDato

    class DokumentInfo(
        tittelHvisTilgjengelig: String?,
        val dokumentInfoId: String,
        val brevkode: String,
        val hovedDokument: Boolean,
    ) {
        val tittel = finnTittel(tittelHvisTilgjengelig)

        private fun finnTittel(tittelHvisTilgjengelig: String?): String =
            if (tittelHvisTilgjengelig != null && tittelHvisTilgjengelig != "null") {
                tittelHvisTilgjengelig
            } else {
                try {
                    brevkode.tilSkjemaType().tittel
                } catch (e: UkjentSkjemaException) {
                    "Ukjent dokumenttittel"
                }
            }

        companion object {
            fun List<DokumentInfo>.vedlegg() = this.filter { it.hovedDokument == false }

            fun List<DokumentInfo>.hovedDokument() = this.find { it.hovedDokument == true } ?: this.first()
        }
    }

    data class RelevantDato(
        val dato: String,
        val datotype: Datotype,
    )

    enum class Datotype {
        DATO_SENDT_PRINT,
        DATO_EKSPEDERT,
        DATO_JOURNALFOERT,
        DATO_REGISTRERT,
        DATO_AVS_RETUR,
        DATO_DOKUMENT,
    }

    data class Bruker(
        val type: BrukerType,
        val id: String,
    ) {
        override fun toString(): String = "Bruker(type=$type, id='<REDACTED>')"
    }

    enum class BrukerType {
        ORGNR,
        AKTOERID,
        FNR,
    }

    fun kategorisertJournalpost(): KategorisertJournalpost {
        if (bruker == null) {
            return UtenBruker(this)
        }
        return when (this.hovedskjema()) {
            in setOf(DAGPENGESØKNAD_ORDINÆR.skjemakode, DAGPENGESØKNAD_PERMITTERT.skjemakode) -> NySøknad(this)
            in setOf(DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR.skjemakode, DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT.skjemakode) -> Gjenopptak(this)
            in setOf(DAGPENGESØKNAD_UTDANNING.skjemakode) -> Utdanning(this)
            in setOf(SkjemaType.DAGPENGESØKNAD_ETABLERING.skjemakode) -> Etablering(this)
            in setOf(KLAGE_OG_ANKE.skjemakode, KLAGE.skjemakode, KLAGE_ETTERSENDING.skjemakode) -> klageType(this)
            in setOf(ANKE.skjemakode, ANKE_ETTERSENDING.skjemakode) -> ankeType(this)
            in
            setOf(
                DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR_ETTERSENDING.skjemakode,
                DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT_ETTERSENDING.skjemakode,
                DAGPENGESØKNAD_ORDINÆR_ETTERSENDING.skjemakode,
                DAGPENGESØKNAD_PERMITTERT_ETTERSENDING.skjemakode,
            ),
            -> Ettersending(this)
            in setOf(GENERELL.skjemakode, GENERELL_FYLLUT.skjemakode) -> Generell(this)
            else -> UkjentSkjemaKode(this)
        }
    }

    private fun klageType(journalpost: Journalpost): KategorisertJournalpost =
        when (journalpost.behandlingstema) {
            "ab0451" -> KlageForskudd(journalpost)
            else -> Klage(journalpost)
        }

    private fun ankeType(journalpost: Journalpost): KategorisertJournalpost = Anke(journalpost)

    override fun toSpesifikkKontekst(): SpesifikkKontekst =
        SpesifikkKontekst(
            "JournalpostData",
            mapOf(
                "journalpostId" to journalpostId(),
            ),
        )

    fun accept(visitor: JournalpostVisitor) {
        visitor.visitJournalpost(journalpostId, journalpostStatus, bruker, behandlingstema, journalførendeEnhet, registrertDato, dokumenter)
    }
}
