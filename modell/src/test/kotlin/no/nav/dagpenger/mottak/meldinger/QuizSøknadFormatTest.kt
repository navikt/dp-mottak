package no.nav.dagpenger.mottak.meldinger

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class QuizSøknadFormatTest {

    @Test
    fun eøsBostedsland() {
        assertFalse(QuizSøknadFormat(bostedsQuizJson("NOR")).eøsBostedsland())
        assertTrue(QuizSøknadFormat(bostedsQuizJson("POL")).eøsBostedsland())
        assertFalse(QuizSøknadFormat(bostedsQuizJson("AGO")).eøsBostedsland())
    }

    @Test
    fun eøsArbeidsforhold() {
        assertTrue(QuizSøknadFormat(eøsArbeidsforholdQuizJson(true)).eøsArbeidsforhold())
        assertFalse(QuizSøknadFormat(eøsArbeidsforholdQuizJson(false)).eøsArbeidsforhold())
    }

    @Test
    fun avtjentVerneplikt() {
        assertTrue(QuizSøknadFormat(vernepliktQuizJson(true)).avtjentVerneplikt())
        assertFalse(QuizSøknadFormat(vernepliktQuizJson(false)).avtjentVerneplikt())
    }

    @Test
    fun `Kan parse avsluttede arbeidsforhold`() {
        assertEquals(
            2,
            QuizSøknadFormat(avsluttedeArbeidsforholdQuizJson()).avsluttetArbeidsforhold().size
        )
    }

    @Test
    fun permittertFraFiskeForedling() {
        assertEquals(
            true,
            QuizSøknadFormat(avsluttedeArbeidsforholdQuizJson(permitterterFraFiskeForedling = true)).permittertFraFiskeForedling()
        )
        assertEquals(
            false,
            QuizSøknadFormat(avsluttedeArbeidsforholdQuizJson(permitterterFraFiskeForedling = false)).permittertFraFiskeForedling()
        )
    }

    @Test
    fun avsluttetArbeidsforholdFraKonkurs() {
        assertEquals(
            true,
            QuizSøknadFormat(avsluttedeArbeidsforholdQuizJson(konkurs = true)).avsluttetArbeidsforholdFraKonkurs()
        )
        assertEquals(
            false,
            QuizSøknadFormat(avsluttedeArbeidsforholdQuizJson(konkurs = false)).avsluttetArbeidsforholdFraKonkurs()
        )
    }

    @Test
    fun permittert() {
        assertEquals(
            true,
            QuizSøknadFormat(avsluttedeArbeidsforholdQuizJson(permittert = true)).permittert()
        )
        assertEquals(
            false,
            QuizSøknadFormat(avsluttedeArbeidsforholdQuizJson(permittert = false)).permittert()
        )
    }
}
