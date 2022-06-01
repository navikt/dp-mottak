package no.nav.dagpenger.mottak.meldinger

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class NyttSøknadFormatTest {

    @Test
    fun eøsBostedsland() {
        assertFalse(NyttSøknadFormat(bostedsQuizJson("NOR")).eøsBostedsland())
        assertTrue(NyttSøknadFormat(bostedsQuizJson("POL")).eøsBostedsland())
        assertFalse(NyttSøknadFormat(bostedsQuizJson("AGO")).eøsBostedsland())
    }

    @Test
    fun eøsArbeidsforhold() {
        assertTrue(NyttSøknadFormat(eøsArbeidsforholdQuizJson(true)).eøsArbeidsforhold())
        assertFalse(NyttSøknadFormat(eøsArbeidsforholdQuizJson(false)).eøsArbeidsforhold())
    }

    @Test
    fun avtjentVerneplikt() {
        assertTrue(NyttSøknadFormat(vernepliktQuizJson(true)).avtjentVerneplikt())
        assertFalse(NyttSøknadFormat(vernepliktQuizJson(false)).avtjentVerneplikt())
    }

    @Test
    fun avsluttetArbeidsforhold() {
    }

    @Test
    fun permittertFraFiskeForedling() {
    }

    @Test
    fun avsluttetArbeidsforholdFraKonkurs() {
    }

    @Test
    fun permittert() {
    }

    @Test
    fun data() {
    }
}
