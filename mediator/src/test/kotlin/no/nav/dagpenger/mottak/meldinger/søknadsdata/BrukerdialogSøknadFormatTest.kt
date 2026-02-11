package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.kotest.assertions.throwables.shouldNotThrowAny
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import org.junit.jupiter.api.Test
import java.util.UUID

class BrukerdialogSøknadFormatTest {
    @Test
    fun `skal validere forventet json`() {
        shouldNotThrowAny {
            BrukerdialogSøknadFormat(løsningJson())
        }

        shouldThrow<IllegalArgumentException> {
            BrukerdialogSøknadFormat(
                jacksonObjectMapper().readTree(
                    //language=JSON
                    """
                    {"hubba": "bubba"}
                    """.trimIndent(),
                ),
            )
        }
    }

    @Test
    fun eøsBostedsland() {
        BrukerdialogSøknadFormat(løsningJson(eøsBostedsland = true)).eøsBostedsland() shouldBe true
        BrukerdialogSøknadFormat(løsningJson(eøsBostedsland = false)).eøsBostedsland() shouldBe false
        BrukerdialogSøknadFormat(tomLøsningJson()).eøsBostedsland() shouldBe false
    }

    @Test
    fun eøsArbeidsforhold() {
        BrukerdialogSøknadFormat(løsningJson(eøsArbeidsforhold = true)).eøsArbeidsforhold() shouldBe true
        BrukerdialogSøknadFormat(løsningJson(eøsArbeidsforhold = false)).eøsArbeidsforhold() shouldBe false
        BrukerdialogSøknadFormat(tomLøsningJson()).eøsArbeidsforhold() shouldBe false
    }

    @Test
    fun avtjentVerneplikt() {
        BrukerdialogSøknadFormat(løsningJson(avtjentVerneplikt = true)).avtjentVerneplikt() shouldBe true
        BrukerdialogSøknadFormat(løsningJson(avtjentVerneplikt = false)).avtjentVerneplikt() shouldBe false
        BrukerdialogSøknadFormat(tomLøsningJson()).avtjentVerneplikt() shouldBe false
    }

    @Test
    fun `Kan parse avsluttede arbeidsforhold`() {
        BrukerdialogSøknadFormat(løsningJson()).avsluttetArbeidsforhold().let {
            it.size shouldBe 2

            it.first() shouldBe
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.SAGT_OPP_AV_ARBEIDSGIVER,
                    fiskeforedling = false,
                    land = "NOR",
                )

            it.last() shouldBe
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT,
                    fiskeforedling = true,
                    land = "SWE",
                )
        }
        BrukerdialogSøknadFormat(tomLøsningJson()).avsluttetArbeidsforhold().shouldBeEmpty()
    }

    @Test
    fun permittertFraFiskeForedling() {
        BrukerdialogSøknadFormat(
            løsningJsonMedAvsluttetArbedsforhold(
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT,
                    fiskeforedling = true,
                    land = "NOR",
                ),
            ),
        ).permittertFraFiskeForedling() shouldBe true

        BrukerdialogSøknadFormat(
            løsningJsonMedAvsluttetArbedsforhold(
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).permittertFraFiskeForedling() shouldBe false

        BrukerdialogSøknadFormat(
            data = tomLøsningJson(),
        ).permittertFraFiskeForedling() shouldBe false
    }

    @Test
    fun avsluttetArbeidsforholdFraKonkurs() {
        BrukerdialogSøknadFormat(
            løsningJsonMedAvsluttetArbedsforhold(
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).avsluttetArbeidsforholdFraKonkurs() shouldBe true
        BrukerdialogSøknadFormat(
            løsningJsonMedAvsluttetArbedsforhold(
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).avsluttetArbeidsforholdFraKonkurs() shouldBe false

        BrukerdialogSøknadFormat(
            data = tomLøsningJson(),
        ).avsluttetArbeidsforholdFraKonkurs() shouldBe false
    }

    @Test
    fun permittert() {
        BrukerdialogSøknadFormat(
            løsningJsonMedAvsluttetArbedsforhold(
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.PERMITTERT,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).permittert() shouldBe true

        BrukerdialogSøknadFormat(
            løsningJsonMedAvsluttetArbedsforhold(
                AvsluttetArbeidsforhold(
                    sluttårsak = AvsluttetArbeidsforhold.Sluttårsak.ARBEIDSGIVER_KONKURS,
                    fiskeforedling = false,
                    land = "NOR",
                ),
            ),
        ).permittert() shouldBe false

        BrukerdialogSøknadFormat(
            data = tomLøsningJson(),
        ).permittert() shouldBe false
    }

    @Test
    fun søknadId() {
        val uuid = UUID.randomUUID()
        BrukerdialogSøknadFormat(
            data = løsningJson(søknadId = uuid),
        ).søknadId() shouldBe uuid.toString()
    }

    private fun løsningJsonMedAvsluttetArbedsforhold(vararg arbeidsforhold: AvsluttetArbeidsforhold): JsonNode {
        val arbeidsforholdJson =
            arbeidsforhold.joinToString(",") {
                //language=JSON
                """
                {
                    "sluttårsak": "${it.sluttårsak.name}",
                    "fiskeforedling": ${it.fiskeforedling},
                    "land": "${it.land}"
                }
                """.trimIndent()
            }
        //language=JSON
        return """
            {
              "@løsning": {
                "Søknadsdata": {
                  "verdi": {
                    "avsluttetArbeidsforhold": [
                      $arbeidsforholdJson
                    ]
                  },
                  "gjelderFra": "2026-01-20"
                }
              }
            }
            """.trimIndent().let {
            jacksonObjectMapper().readTree(it)
        }
    }

    private fun løsningJson(
        eøsBostedsland: Boolean = false,
        eøsArbeidsforhold: Boolean = false,
        avtjentVerneplikt: Boolean = false,
        søknadId: UUID = UUID.randomUUID(),
    ): JsonNode {
        //language=JSON
        return """
            {
              "@event_name": "behov",
              "ident": "12345678910",
              "søknadId": "9fbfd212-4453-44e0-8b24-2a02ba9bd007",
              "@behov": [
                "Søknadsdata"
              ],
              "journalpostId": "12345679",
              "@id": "68efcd4e-c0bc-4b94-8ad2-c3737e82ce52",
              "@opprettet": "2026-01-20T22:41:23.554212",
              "system_read_count": 0,
              "system_participating_services": [
                {
                  "id": "68efcd4e-c0bc-4b94-8ad2-c3737e82ce52",
                  "time": "2026-01-20T22:41:23.554212"
                }
              ],
              "@løsning": {
                "Søknadsdata": {
                  "verdi": {
                    "eøsBostedsland": $eøsBostedsland,
                    "eøsArbeidsforhold": $eøsArbeidsforhold ,
                    "avtjentVerneplikt": $avtjentVerneplikt,
                    "avsluttetArbeidsforhold": [
                      {
                        "sluttårsak": "SAGT_OPP_AV_ARBEIDSGIVER",
                        "fiskeforedling": false,
                        "land": "NOR"
                      },
                      {
                        "sluttårsak": "PERMITTERT",
                        "fiskeforedling": true,
                        "land": "SWE"
                      }
                    ],
                    "harBarn": false,
                    "harAndreYtelser": false,
                    "ønskerDagpengerFraDato": "2026-01-20",
                    "søknadId": "$søknadId",
                    "reellArbeidssøker": {
                      "helse": false,
                      "geografi": false,
                      "deltid": false,
                      "yrke": false
                    }
                  },
                  "gjelderFra": "2026-01-20"
                }
              }
            }
            
            """.trimIndent().let {
            jacksonObjectMapper().readTree(it)
        }
    }

    private fun tomLøsningJson(): JsonNode {
        //language=JSON
        return """
            {
              "@løsning": {
                "Søknadsdata": {
                  "verdi": {
                  },
                  "gjelderFra": "2026-01-20"
                }
              }
            }
            """.trimIndent().let {
            jacksonObjectMapper().readTree(it)
        }
    }
}
