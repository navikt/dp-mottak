package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.meldinger.søknadsdata.QuizSøknadFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate
import java.util.UUID

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
    fun `kan parse tom arbeidsforhold`() {
        assertEquals(
            0,
            QuizSøknadFormat(tomAvsluttedeArbeidsforhold()).avsluttetArbeidsforhold().size
        )
    }

    @Test
    fun `skal kunne parse delvise utfylt arbeidsforhold`() {
        assertEquals(
            2,
            QuizSøknadFormat(delvisutfyltArbeidsforhold()).avsluttetArbeidsforhold().size
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
        assertDoesNotThrow {
            assertEquals(
                false,
                QuizSøknadFormat(utenArbeidsforholdQuizJson()).avsluttetArbeidsforholdFraKonkurs()
            )
        }
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

    @Test
    fun fangstOgFiske() {
        assertEquals(
            false,
            QuizSøknadFormat(utenSeksjoner()).fangstOgFisk()
        )
    }

    @Test
    fun ønskerDagpengerFraDato() {
        val now = LocalDate.now()
        assertEquals(
            now,
            QuizSøknadFormat(ønskerDagpengerFraDatoJson(now.toString())).ønskerDagpengerFraDato()
        )
    }

    @Test
    fun søknadsId() {
        val uuid = UUID.randomUUID()
        assertEquals(
            uuid.toString(),
            QuizSøknadFormat(utenSeksjoner(uuid)).søknadsId()
        )
    }
}
