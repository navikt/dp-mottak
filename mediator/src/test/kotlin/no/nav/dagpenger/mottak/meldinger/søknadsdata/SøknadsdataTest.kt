package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.matchers.types.shouldBeInstanceOf
import org.junit.jupiter.api.Test

internal class SøknadsdataTest {
    private val objektMapper = jacksonObjectMapper()

    @Test
    fun `skal returnere QuizSøknadFormat når versjon_navn er Dagpenger`() {
        rutingOppslag(objektMapper.readTree("""{ "versjon_navn": "Dagpenger" }"""))
            .shouldBeInstanceOf<QuizSøknadFormat>()
    }

    @Test
    fun `skal returnere BrukerdialogSøknadFormat når verdi er objekt`() {
        rutingOppslag(
            objektMapper.readTree(
                """
                    {
                      "@løsning": {
                        "Søknadsdata": {
                          "verdi": { "foo": "bar" }
                        }
                      }
                    }
                """,
            ),
        ).shouldBeInstanceOf<BrukerdialogSøknadFormat>()
    }

    @Test
    fun `skal returnere NullSøknadData når ingen format matcher`() {
        rutingOppslag(objektMapper.readTree("""{ "annen": "verdi" }""")).shouldBeInstanceOf<NullSøknadData>()
    }
}
