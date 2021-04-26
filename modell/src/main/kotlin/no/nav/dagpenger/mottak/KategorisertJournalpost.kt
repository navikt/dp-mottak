package no.nav.dagpenger.mottak

import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonMapperBuilder
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon.Person
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

private const val maksTegn = 1999

sealed class KategorisertJournalpost(
    open val journalpost: Journalpost
) {
    protected abstract fun henvendelseNavn(): String
    protected open fun finnOppgaveBenk(
        søknadFakta: SøknadFakta?,
        oppfyllerMinsteArbeidsinntekt: Boolean?,
        person: Person?
    ): OppgaveBenk =
        OppgaveBenk(behandlendeEnhet(person), henvendelseNavn(), journalpost.datoRegistrert(), tilleggsinformasjon())

    protected fun behandlendeEnhet(person: Person?): String {
        val brevkode = journalpost.dokumenter().firstOrNull()?.brevkode ?: "ukjent"
        return when {
            brevkode in PERMITTERING_BREVKODER && person?.norskTilknytning == false -> "4465"
            brevkode in PERMITTERING_BREVKODER -> "4455"
            brevkode in UTLAND_BREVKODER -> "4470"
            else -> "4450"
        }
    }

    internal fun oppgaveBenk(
        person: Person?,
        søknadFakta: SøknadFakta? = null,
        oppfyllerMinsteArbeidsinntekt: Boolean? = null
    ): OppgaveBenk {
        val oppgaveBenk = finnOppgaveBenk(søknadFakta, oppfyllerMinsteArbeidsinntekt, person)

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
        val titler = journalpost.dokumenter().map { it.tittel }
        val hovedDokument = titler.first()
        val vedlegg = titler.drop(1)

        val formatertVedlegg =
            if (vedlegg.isNotEmpty()) {
                vedlegg.joinToString(prefix = "- ", separator = "\n- ", postfix = "\n")
            } else {
                ""
            }

        val formatertDato = journalpost.datoRegistrert().toLocalDate().format(DateTimeFormatter.ofPattern("dd.MM.yyyy"))
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
    ) {
        private companion object {
            val objectMapper = jacksonMapperBuilder().addModule(JavaTimeModule()).build()
        }
        fun toJson() = objectMapper.writeValueAsString(this)
    }
}

data class NySøknad(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String =
        "Start Vedtaksbehandling - automatisk journalført.\n"

    override fun finnOppgaveBenk(
        søknadFakta: SøknadFakta?,
        oppfyllerMinsteArbeidsinntekt: Boolean?,
        person: Person?
    ): OppgaveBenk {
        // val koronaRegelverkMinsteinntektBrukt =
        //     packet.getNullableBoolean(PacketKeys.KORONAREGELVERK_MINSTEINNTEKT_BRUKT) == true
        val konkurs = søknadFakta?.harAvsluttetArbeidsforholdFraKonkurs() == true
        val kanAvslåsPåMinsteinntekt = oppfyllerMinsteArbeidsinntekt == false
        val grenseArbeider = søknadFakta?.erGrenseArbeider() == true
        val eøsArbeidsforhold = søknadFakta?.harEøsArbeidsforhold() == true
        val inntektFraFangstFisk = søknadFakta?.harInntektFraFangstOgFiske() == true
        val harAvtjentVerneplikt = søknadFakta?.harAvtjentVerneplikt() == true
        val erPermittertFraFiskeforedling = søknadFakta?.erPermittertFraFiskeForedling() == true
        val datoRegistrert = journalpost.datoRegistrert()
        val originalOppgavebenk = when {
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
        return fornyetRettEllerOrginal(søknadFakta, originalOppgavebenk)
    }

    private fun finnEnhetForHurtigAvslag(person: Person?) = when (behandlendeEnhet(person)) {
        "4450" -> "4451"
        "4455" -> "4456"
        else -> behandlendeEnhet(person)
    }
}

data class Gjenopptak(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "Gjenopptak\n"

    override fun finnOppgaveBenk(
        søknadFakta: SøknadFakta?,
        oppfyllerMinsteArbeidsinntekt: Boolean?,
        person: Person?
    ) = fornyetRettEllerOrginal(søknadFakta, super.finnOppgaveBenk(søknadFakta, oppfyllerMinsteArbeidsinntekt, person))
}

data class Utdanning(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "Utdanning\n"
}

data class Etablering(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "Etablering\n"
}

data class KlageOgAnke(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "Klage og anke\n"
}

data class KlageOgAnkeLønnskompensasjon(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "Klage og anke — Lønnskompensasjon\n"
    override fun finnOppgaveBenk(
        søknadFakta: SøknadFakta?,
        oppfyllerMinsteArbeidsinntekt: Boolean?,
        person: Person?
    ) = OppgaveBenk(
        beskrivelse = henvendelseNavn(),
        id = "4486",
        datoRegistrert = journalpost.datoRegistrert(),
        tilleggsinformasjon = tilleggsinformasjon()
    )
}

data class Ettersending(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "Ettersending\n"
}

data class UkjentSkjemaKode(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "${journalpost.dokumenter().first().tittel}\n"
}

data class UtenBruker(
    override val journalpost: Journalpost
) : KategorisertJournalpost(journalpost) {
    override fun henvendelseNavn(): String = "${journalpost.dokumenter().first().tittel}\n"
}

private fun fornyetRettEllerOrginal(
    søknadFakta: SøknadFakta?,
    originalOppgavebenk: KategorisertJournalpost.OppgaveBenk
): KategorisertJournalpost.OppgaveBenk {
    return if (søknadFakta?.erFornyetRettighet() == true)
        KategorisertJournalpost.OppgaveBenk(
            "4451",
            "Anmodningsvedtak 538",
            originalOppgavebenk.datoRegistrert,
            originalOppgavebenk.toJson()
        ) else originalOppgavebenk
}
