package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.SpesifikkKontekst
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val maksTegn = 1999

class JournalpostData(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val journalpostStatus: String,
    private val aktørId: String, // Bruker? (kan vi få fnr her?)
    private val relevanteDatoer: List<RelevantDato>,
    private val dokumenter: List<DokumentInfo>
) : Hendelse(aktivitetslogg) {

    override fun journalpostId(): String = journalpostId
    fun aktørId(): String = aktørId

    class DokumentInfo(kanskjetittel: String?, dokumentInfoId: String, brevkode: String) {
        val tittel = kanskjetittel ?: allKnownTypes[brevkode] ?: "Ukjent dokumenttittel"
        val dokumentInfoId = dokumentInfoId
        val brevkode = brevkode
    }

    data class RelevantDato(
        val dato: String,
        val datotype: Datotype
    )

    enum class Datotype {
        DATO_SENDT_PRINT, DATO_EKSPEDERT, DATO_JOURNALFOERT,
        DATO_REGISTRERT, DATO_AVS_RETUR, DATO_DOKUMENT
    }

    fun journalpost(): KategorisertJournalpost {

        val brevkode = dokumenter.firstOrNull()?.brevkode
        val jpDokumenter =
            dokumenter.map { KategorisertJournalpost.Dokument(it.tittel, it.dokumentInfoId, it.brevkode) }
        val oslo = ZoneId.of("Europe/Oslo")
        val datoRegistrert = relevanteDatoer.find { dato -> dato.datotype == Datotype.DATO_REGISTRERT }?.let {
            LocalDateTime.parse(it.dato).atZone(oslo)
        } ?: LocalDateTime.now().atZone(oslo)

        return when (brevkode) {
            in listOf("NAV 04-01.03", "NAV 04-01.04") -> KategorisertJournalpost.NySøknad(
                journalpostId = journalpostId,
                journalpostStatus = journalpostStatus,
                dokumenter = jpDokumenter,
                datoRegistrert = datoRegistrert
            )
            else -> TODO("Ikke kategorisert flere enn ny søknad")
        }
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst(
        "JournalpostData",
        mapOf(
            "aktørId" to aktørId(),
            "journalpostId" to journalpostId()
        )
    )

    internal companion object {

        // @todo: not sure if this belong here
        val allKnownTypes = mapOf(
            "NAV 04-02.03" to "Bekreftelse på ansettelsesforhold",
            "O2" to "Arbeidsavtale",
            "M6" to "Timelister",
            "M7" to " Brev fra bobestyrer/konkursforvalter",
            "NAV 04-02.05" to "Søknad om attest PD U1/N-301 til bruk ved overføring av dagpengerettigheter",
            "NAVe 04-02.05" to "Ettersendelse til søknad om attest PD U1/N-301 til bruk ved overføring av dagpengerettigheter",
            "NAV 04-08.03" to "Bekreftelse på sluttårsak/nedsatt arbeidstid (ikke permittert)",
            "NAVe 04-08.03" to "Ettersendelse til bekreftelse på sluttårsak/nedsatt arbeidstid (ikke permittert)",
            "S7" to "Kopi av arbeidsavtale/sluttårsak",
            "NAV 04-08.04" to "Bekreftelse på arbeidsforhold og permittering",
            "NAVe 04-08.04" to "Ettersendelse til bekreftelse på arbeidsforhold og permittering",
            "T3" to "Tjenestebevis",
            "S6" to "Dokumentasjon av sluttårsak",
            "NAV 04-16.04" to "Søknad om gjenopptak av dagpenger ved permittering",
            "NAVe 04-16.04" to "Ettersendelse til søknad om gjenopptak av dagpenger ved permittering",
            "NAV 04-02.01" to "Søknad om utstedelse av attest PD U2",
            "NAVe 04-02.01" to "Ettersendelse til søknad om attest PD U2",
            "O9" to "Bekreftelse fra studiested/skole",
            "NAV 04-06.05" to "Søknad om godkjenning av utdanning med rett til dagpenger",
            "NAVe 04-06.05" to "Ettersendelse til søknad om godkjenning av utdanning med rett til dagpenger",
            "N2" to "Kopi av søknad",
            "N5" to "Kopi av undersøkelsesresultat",
            "NAV 04-06.08" to "Søknad om dagpenger under etablering av egen virksomhet",
            "NAVe 04-06.08" to "Ettersendelse til søknad om dagpenger under etablering av egen virksomhet",
            "NAV 04-16.03" to "Søknad om gjenopptak av dagpenger",
            "NAVe 04-16.03" to "Ettersendelse til søknad om gjenopptak av dagpenger (ikke permittert)",
            "NAV 04-13.01" to "Egenerklæringsskjema for fastsettelse av grensearbeiderstatus",
            "NAV 04-03.07" to "Egenerklæring - overdragelse av lønnskrav",
            "NAVe 04-03.07" to "Ettersendelse til egenerklæring - overdragelse av lønnskrav ved konkurs mv",
            "X8" to "Fødselsattest/bostedsbevis for barn under 18 år",
            "T5" to "SED U006 Familieinformasjon",
            "T4" to "Oppholds- og arbeidstillatelse, eller registreringsbevis for EØS-borgere",
            "T2" to "Dokumentasjon av sluttdato",
            "T1" to "Elevdokumentasjon fra lærested",
            "V6" to "Kopi av sluttavtale",
            "U1" to "U1 Perioder av betydning for retten til dagpenger",
            "NAV 04-03.08" to "Oversikt over arbeidstimer",
            "NAVe 04-03.08" to "Ettersendelse til oversikt over arbeidstimer",
            "S8" to "Sjøfartsbok/hyreavregning",
            "NAV 04-01.03" to "Søknad om dagpenger (ikke permittert)",
            "NAVe 04-01.03" to "Ettersendelse til søknad om dagpenger ved arbeidsledighet (ikke permittert)",
            "NAV 04-01.04" to "Søknad om dagpenger ved permittering",
            "NAVe 04-01.04" to "Ettersendelse til søknad om dagpenger ved permittering",
            "NAV 90-00.08" to "Klage og anke"
        )
    }

    sealed class KategorisertJournalpost {
        abstract fun journalpostId(): String
        abstract fun journalpostStatus(): String
        abstract fun dokumenter(): List<Dokument>
        abstract fun datoRegistrert(): ZonedDateTime

        data class NySøknad(
            val journalpostId: String,
            val journalpostStatus: String,
            val dokumenter: List<Dokument>,
            val datoRegistrert: ZonedDateTime
        ) : KategorisertJournalpost() {
            override fun journalpostId(): String = journalpostId
            override fun journalpostStatus(): String = journalpostStatus
            override fun dokumenter(): List<Dokument> = dokumenter
            override fun datoRegistrert(): ZonedDateTime = datoRegistrert
        }

        data class Dokument(val tittel: String, val dokumentInfoId: String, val brevkode: String)
    }
}

fun JournalpostData.KategorisertJournalpost.tilleggsinformasjon(): String {
    val titler = dokumenter().map { it.tittel }
    val hovedDokument = titler.first()
    val vedlegg = titler.drop(1)

    val formatertVedlegg =
        if (vedlegg.isNotEmpty()) {
            vedlegg.joinToString(prefix = "- ", separator = "\n- ", postfix = "\n")
        } else {
            ""
        }

    val formatertDato = datoRegistrert().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
    val datoBeskrivelse = "Registrert dato: ${formatertDato}\n"

    val informasjon = "Hoveddokument: ${hovedDokument}\n" +
        formatertVedlegg +
        datoBeskrivelse +
        "Dokumentet er skannet inn og journalført automatisk av digitale dagpenger. Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\"."

    return if (informasjon.length > maksTegn) {
        "Hoveddokument: ${hovedDokument}\nRegistrert dato: ${formatertDato}\nDokumentet er skannet inn og journalført automatisk av digitale dagpenger. Gjennomfør rutinen \"Etterkontroll av automatisk journalførte dokumenter\"."
    } else {
        informasjon
    }
}
