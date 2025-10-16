package no.nav.dagpenger.mottak.meldinger

enum class SkjemaType(val skjemakode: String, val tittel: String) {
    DAGPENGESØKNAD_ORDINÆR("NAV 04-01.03", "Søknad om dagpenger (ikke permittert)"),
    DAGPENGESØKNAD_ORDINÆR_ETTERSENDING("NAVe 04-01.03", "Ettersendelse til søknad om dagpenger ved arbeidsledighet (ikke permittert)"),
    DAGPENGESØKNAD_PERMITTERT("NAV 04-01.04", "Søknad om dagpenger ved permittering"),
    DAGPENGESØKNAD_PERMITTERT_ETTERSENDING("NAVe 04-01.04", "Ettersendelse til søknad om dagpenger ved permittering"),
    DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR("NAV 04-16.03", "Søknad om gjenopptak av dagpenger"),
    DAGPENGESØKNAD_GJENOPPTAK_ORDINÆR_ETTERSENDING("NAVe 04-16.03", "Ettersendelse til søknad om gjenopptak av dagpenger (ikke permittert)"),
    DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT("NAV 04-16.04", "Søknad om gjenopptak av dagpenger ved permittering"),
    DAGPENGESØKNAD_GJENOPPTAK_PERMITTERT_ETTERSENDING("NAVe 04-16.04", "Ettersendelse til søknad om gjenopptak av dagpenger ved permittering"),
    DAGPENGESØKNAD_UTDANNING("NAV 04-06.05", "Søknad om godkjenning av utdanning med rett til dagpenger"),
    DAGPENGESØKNAD_ETABLERING("NAV 04-06.08", "Søknad om dagpenger under etablering av egen virksomhet"),
    KLAGE_OG_ANKE("NAV 90-00.08", "Klage og anke"),
    KLAGE("NAV 90-00.08 K", "Klage"),
    KLAGE_ETTERSENDING("NAVe 90-00.08 K", "Ettersendelse til klage"),
    ANKE("NAV 90-00.08 A", "Anke"),
    ANKE_ETTERSENDING("NAVe 90-00.08 A", "Ettersendelse til anke"),
    GENERELL("GENERELL_INNSENDING", "Generell innsending"),
    DOKUMENTASJON_ANDRE_YTELSER("K1", "Dokumentasjon av andre ytelser"),
    TIMELISTER("M6", "Timelister"),
    BREV_FRA_BOBESTYRER_KONKURSFORVALTER("M7", "Brev fra bobestyrer/konkursforvalter"),
    KOPI_AV_SØKNAD("N2", "Kopi av søknad"),
    KOPI_AV_UNDERSØKELSESRESULTAT("N5", "Kopi av undersøkelsesresultat"),
    SØKNAD_OM_UTSTEDELSE_AV_ATTEST_PD_U2("NAV 04-02.01", "Søknad om utstedelse av attest PD U2"),
    BEKREFTELSE_PÅ_ANSETTELSESFORHOLD("NAV 04-02.03", "Bekreftelse på ansettelsesforhold"),
    SØKNAD_OM_ATTEST_PD_U1_N_301_TIL_BRUK_VED_OVERFØRING_AV_DAGPENGERETTIGHETER("NAV 04-02.05", "Søknad om attest PD U1/N-301 til bruk ved overføring av dagpengerettigheter"),
    EGENERKLÆRING_OVERDRAGELSE_AV_LØNNSKRAV("NAV 04-03.07", "Egenerklæring - overdragelse av lønnskrav"),
    OVERSIKT_OVER_ARBEIDSTIMER("NAV 04-03.08", "Oversikt over arbeidstimer"),
    BEKREFTELSE_PÅ_SLUTTÅRSAK_NEDSATT_ARBEIDSTID_IKKE_PERMITTERT("NAV 04-08.03", "Bekreftelse på sluttårsak/nedsatt arbeidstid (ikke permittert)"),
    BEKREFTELSE_PÅ_ARBEIDSFORHOLD_OG_PERMITTERING("NAV 04-08.04", "Bekreftelse på arbeidsforhold og permittering"),
    EGENERKLÆRING_FASTSETTELSE_AV_GRENSEARBEIDERSTATUS("NAV 04-13.01", "Egenerklæringsskjema for fastsettelse av grensearbeiderstatus"),
    ETTERSENDELSE_TIL_SØKNAD_OM_ATTEST_PD_U2("NAVe 04-02.01", "Ettersendelse til søknad om attest PD U2"),
    ETTERSENDELSE_TIL_SØKNAD_OM_ATTEST_PD_U1_N_301_TIL_BRUK_VED_OVERFØRING_AV_DAGPENGERETTIGHETER("NAVe 04-02.05", "Ettersendelse til søknad om attest PD U1/N-301 til bruk ved overføring av dagpengerettigheter"),
    ETTERSENDELSE_TIL_EGENERKLÆRING_OVERDRAGELSE_AV_LØNNSKRAV_VED_KONKURS("NAVe 04-03.07", "Ettersendelse til egenerklæring - overdragelse av lønnskrav ved konkurs mv"),
    ETTERSENDELSE_TIL_OVERSIKT_OVER_ARBEIDSTIMER("NAVe 04-03.08", "Ettersendelse til oversikt over arbeidstimer"),
    ETTERSENDELSE_TIL_BEKREFTELSE_PÅ_SLUTTÅRSAK_NEDSATT_ARBEIDSTID_IKKE_PERMITTERT("NAVe 04-08.03", "Ettersendelse til bekreftelse på sluttårsak/nedsatt arbeidstid (ikke permittert)"),
    ETTERSENDELSE_TIL_SØKNAD_OM_ETABLERING_AV_EGEN_VIRKSOMHET("NAVe 04-06.08", "Ettersendelse til bekreftelse på sluttårsak/nedsatt arbeidstid (ikke permittert)"),
    ETTERSENDELSE_TIL_BEKREFTELSE_PÅ_ARBEIDSFORHOLD_OG_PERMITTERING("NAVe 04-08.04", "Ettersendelse til bekreftelse på arbeidsforhold og permittering"),
    ARBEIDSAVTALE("O2", "Arbeidsavtale"),
    BEKREFTELSE_FRA_STUDIESTED_SKOLE("O9", "Bekreftelse fra studiested/skole"),
    DOKUMENTASJON_AV_SLUTTÅRSAK("S6", "Dokumentasjon av sluttårsak"),
    KOPI_AV_ARBEIDSAVTALE_SLUTTÅRSAK("S7", "Kopi av arbeidsavtale/sluttårsak"),
    SJØFARTSBOK_HYREAVREGNING("S8", "Sjøfartsbok/hyreavregning"),
    ELEVDOKUMENTASJON_FRA_LÆRESTED("T1", "Elevdokumentasjon fra lærested"),
    DOKUMENTASJON_AV_SLUTTDATO("T2", "Dokumentasjon av sluttdato"),
    TJENESTEBEVIS("T3", "Tjenestebevis"),
    OPPHOLDS_OG_ARBEIDSTILLATELSE_ELLER_REGISTRERINGSBEVIS_FOR_EØS_BORGERE("T4", "Oppholds- og arbeidstillatelse, eller registreringsbevis for EØS-borgere"),
    SED_U006_FAMILIEINFORMASJON("T5", "SED U006 Familieinformasjon"),
    PERMITTERINGSVARSEL("T6", "Permitteringsvarsel"),
    DOKUMENTASJON_AV_ARBEIDSFORHOLD("T8", "Dokumentasjon av arbeidsforhold"),
    DOKUMENTASJON_AV_HELSE_OG_FUNKSJONSNIVÅ("T9", "Dokumentasjon av helse og funksjonsnivå"),
    U1_PERIODER_AV_BETYDNING_FOR_RETTEN_TIL_DAGPENGER("U1", "U1 Perioder av betydning for retten til dagpenger"),
    KOPI_AV_SLUTTAVTALE("V6", "Kopi av sluttavtale"),
    FØDSELATTEST_BOSTEDSBEVIS_FOR_BARN_UNDER_18_ÅR("X8", "Fødselsattest/bostedsbevis for barn under 18 år"),
    UTTALELSE_ELLER_VURDERING_FRA_KOMPETENT_FAGPERSONELL("XY", "Uttalelse eller vurdering fra kompetent fagpersonell"),
    MELDEKORT("NAV 00-10.02", "Meldekort"),
    ;

    companion object {
        fun String.tilSkjemaType(): SkjemaType {
            return entries.firstOrNull { it.skjemakode == this }
                ?: throw UkjentSkjemaException("Ukjent skjemakode: $this")
        }
    }
}

class UkjentSkjemaException(message: String) : RuntimeException(message)
