package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon.Person
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val maksTegn = 1999

sealed class KategorisertJournalpost(
    open val journalpostData: JournalpostData
) {
    protected abstract fun henvendelseNavn(): String
    protected open fun finnOppgaveBenk(
        søknad: Søknad?,
        oppfyllerMinsteArbeidsinntekt: Boolean?,
        person: Person?
    ): OppgaveBenk =
        OppgaveBenk(behandlendeEnhet(person), henvendelseNavn(), journalpostData.datoRegistrert(), tilleggsinformasjon())

    protected fun behandlendeEnhet(person: Person?): String {
        val brevkode = journalpostData.dokumenter().firstOrNull()?.brevkode ?: "ukjent"
        return when {
            brevkode in PERMITTERING_BREVKODER && person?.norskTilknytning == false -> "4465"
            brevkode in PERMITTERING_BREVKODER -> "4455"
            brevkode in UTLAND_BREVKODER -> "4470"
            else -> "4450"
        }
    }

    internal fun oppgaveBenk(
        person: Person?,
        søknad: Søknad? = null,
        oppfyllerMinsteArbeidsinntekt: Boolean? = null
    ): OppgaveBenk {
        val oppgaveBenk = finnOppgaveBenk(søknad, oppfyllerMinsteArbeidsinntekt, person)

        return when (person?.diskresjonskode) {
            true -> oppgaveBenk.copy(
                id = "2103",
                beskrivelse = henvendelseNavn()
            )
            else -> oppgaveBenk
        }
    }

    private val PERMITTERING_BREVKODER =
        listOf(
            "NAV 04-01.04",
            "NAVe 04-01.04",
            "NAV 04-16.04",
            "NAVe 04-16.04",
            "NAVe 04-08.04",
            "NAV 04-08.04"
        )

    private val UTLAND_BREVKODER =
        listOf("NAV 04-02.01", "NAVe 04-02.01", "NAV 04-02.03", "NAV 04-02.05", "NAVe 04-02.05")

    fun tilleggsinformasjon(): String {
        val titler = journalpostData.dokumenter().map { it.tittel }
        val hovedDokument = titler.first()
        val vedlegg = titler.drop(1)

        val formatertVedlegg =
            if (vedlegg.isNotEmpty()) {
                vedlegg.joinToString(prefix = "- ", separator = "\n- ", postfix = "\n")
            } else {
                ""
            }

        val formatertDato = journalpostData.datoRegistrert().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
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

    data class OppgaveBenk(
        val id: String,
        val beskrivelse: String,
        val datoRegistrert: ZonedDateTime,
        val tilleggsinformasjon: String
    )
}

data class NySøknad(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String =
        "Start Vedtaksbehandling - automatisk journalført.\n"

    override fun finnOppgaveBenk(
        søknad: Søknad?,
        oppfyllerMinsteArbeidsinntekt: Boolean?,
        person: Person?
    ): OppgaveBenk {
        requireNotNull(søknad) { " Søknadsdata må være satt på dette tidspunktet" }
        // val koronaRegelverkMinsteinntektBrukt =
        //     packet.getNullableBoolean(PacketKeys.KORONAREGELVERK_MINSTEINNTEKT_BRUKT) == true
        val konkurs = søknad.harAvsluttetArbeidsforholdFraKonkurs()
        val kanAvslåsPåMinsteinntekt = oppfyllerMinsteArbeidsinntekt == false
        val grenseArbeider = søknad.erGrenseArbeider()
        val eøsArbeidsforhold = søknad.harEøsArbeidsforhold()
        val inntektFraFangstFisk = søknad.harInntektFraFangstOgFiske()
        val harAvtjentVerneplikt = søknad.harAvtjentVerneplikt()
        val erPermittertFraFiskeforedling = søknad.erPermittertFraFiskeForedling()
        val datoRegistrert = journalpostData.datoRegistrert()
        return when {
            eøsArbeidsforhold -> {
                OppgaveBenk(
                    "4470",
                    "MULIG SAMMENLEGGING - EØS\n",
                    datoRegistrert,
                    tilleggsinformasjon()
                )
            }
            harAvtjentVerneplikt -> OppgaveBenk(
                behandlendeEnhet(person),
                "VERNEPLIKT\n",
                datoRegistrert,
                tilleggsinformasjon()
            )
            inntektFraFangstFisk -> OppgaveBenk(
                behandlendeEnhet(person),
                "FANGST OG FISKE\n",
                datoRegistrert,
                tilleggsinformasjon()
            )
            grenseArbeider -> OppgaveBenk(
                "4465", "EØS\n",
                datoRegistrert,
                tilleggsinformasjon()
            )
            konkurs -> OppgaveBenk(
                "4401", "Konkurs\n",
                datoRegistrert,
                tilleggsinformasjon()
            )
            erPermittertFraFiskeforedling -> OppgaveBenk(
                "4454", "FISK\n",
                datoRegistrert,
                tilleggsinformasjon()
            )
            kanAvslåsPåMinsteinntekt -> OppgaveBenk(
                finnEnhetForHurtigAvslag(person),
                "Minsteinntekt - mulig avslag\n", // if (koronaRegelverkMinsteinntektBrukt) "Minsteinntekt - mulig avslag - korona\n" else "Minsteinntekt - mulig avslag\n"
                datoRegistrert,
                tilleggsinformasjon()
            )
            else -> OppgaveBenk(
                behandlendeEnhet(person),
                henvendelseNavn(),
                datoRegistrert,
                tilleggsinformasjon()
            )
        }
    }

    private fun finnEnhetForHurtigAvslag(person: Person?) = when (behandlendeEnhet(person)) {
        "4450" -> "4451"
        "4455" -> "4456"
        else -> behandlendeEnhet(person)
    }
}

data class Gjenopptak(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "Gjenopptak\n"
}

data class Utdanning(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "Utdanning\n"
}

data class Etablering(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "Etablering\n"
}

data class KlageOgAnke(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "Klage og anke\n"
}

data class KlageOgAnkeLønnskompensasjon(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "Klage og anke — Lønnskompensasjon\n"
    override fun finnOppgaveBenk(
        søknad: Søknad?,
        oppfyllerMinsteArbeidsinntekt: Boolean?,
        person: Person?
    ) = OppgaveBenk(
        beskrivelse = henvendelseNavn(),
        id = "4486",
        datoRegistrert = journalpostData.datoRegistrert(),
        tilleggsinformasjon = tilleggsinformasjon()
    )
}

data class Ettersending(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "Ettersending\n"
}

data class UkjentSkjemaKode(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "${journalpostData.dokumenter().first().tittel}\n"
}

data class UtenBruker(
    override val journalpostData: JournalpostData
) : KategorisertJournalpost(journalpostData) {
    override fun henvendelseNavn(): String = "${journalpostData.dokumenter().first().tittel}\n"
}
