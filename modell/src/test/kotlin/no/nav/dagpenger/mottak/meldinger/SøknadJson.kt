package no.nav.dagpenger.mottak.meldinger

import no.nav.dagpenger.mottak.toJsonNode
import org.intellij.lang.annotations.Language

fun vernepliktQuizJson(harAvtjentVerneplikt: Boolean) =
    //language=json
    """{
  "fødselsnummer": "12020052345",
  "@event_name": "søker_oppgave",
  "versjon_id": -1,
  "versjon_navn": "Dagpenger",
  "@opprettet": "2022-06-01T10:58:42.361701",
  "@id": "e40a47fc-e0ee-41ab-92f6-4bac98162753",
  "søknad_uuid": "cfd84357-cdd9-4811-ada5-63d77625e91e",
  "ferdig": false,
  "seksjoner": [
    {
      "beskrivendeId": "verneplikt",
      "fakta": [
        {
          "id": "7001",
          "type": "boolean",
          "beskrivendeId": "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd",
          "svar": $harAvtjentVerneplikt,
          "roller": [
            "søker"
          ],
          "gyldigeValg": [
            "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd.svar.ja",
            "faktum.avtjent-militaer-sivilforsvar-tjeneste-siste-12-mnd.svar.nei"
          ]
        }
      ]
    }
  ]
}""".toJsonNode()

fun bostedsQuizJson(land: String) =
    //language=JSON
    """{
  "fødselsnummer": "12020052345",
  "@event_name": "søker_oppgave",
  "versjon_id": -1,
  "versjon_navn": "Dagpenger",
  "@opprettet": "2022-06-01T10:32:09.866832",
  "@id": "24ab2cb0-5dc3-4f19-acf4-98547730706a",
  "søknad_uuid": "c725ce65-cfd5-4242-932c-b3b50fe82e57",
  "ferdig": false,
  "seksjoner": [
    {
      "beskrivendeId": "bostedsland",
      "fakta": [
        {
          "id": "6001",
          "type": "land",
          "beskrivendeId": "faktum.hvilket-land-bor-du-i",
          "svar": "$land",
          "roller": [
            "søker"
          ]
        }
      ]
    }
  ]
}""".toJsonNode()

fun utenArbeidsforholdQuizJson() =
    //language=JSON
    """{
  "fødselsnummer": "12020052345",
  "@event_name": "søker_oppgave",
  "versjon_id": -1,
  "versjon_navn": "Dagpenger",
  "@opprettet": "2022-06-01T10:32:09.866832",
  "@id": "24ab2cb0-5dc3-4f19-acf4-98547730706a",
  "søknad_uuid": "c725ce65-cfd5-4242-932c-b3b50fe82e57",
  "ferdig": false,
  "seksjoner": [
    {
      "beskrivendeId": "arbeidsforhold",
      "fakta": [
        {
          "id": "8002",
          "svar": "faktum.type-arbeidstid.svar.ingen-passer",
          "type": "envalg",
          "roller": [
            "søker"
          ],
          "readOnly": false,
          "gyldigeValg": [
            "faktum.type-arbeidstid.svar.fast",
            "faktum.type-arbeidstid.svar.varierende",
            "faktum.type-arbeidstid.svar.kombinasjon",
            "faktum.type-arbeidstid.svar.ingen-passer"
          ],
          "beskrivendeId": "faktum.type-arbeidstid"
        }
      ]
    }
  ]
}""".toJsonNode()

fun eøsArbeidsforholdQuizJson(harEøsarbeidsforhold: Boolean) =
    //language=json
    """{
  "fødselsnummer": "12020052345",
  "@event_name": "søker_oppgave",
  "versjon_id": 0,
  "versjon_navn": "test",
  "@opprettet": "2022-05-27T09:03:14.745705",
  "@id": "5294a832-346c-4136-9666-cb6db09c7251",
  "søknad_uuid": "802c6b17-2f96-4cb6-b090-938912f99bc6",
  "ferdig": true,
  "seksjoner": [
    {
      "beskrivendeId": "eos-arbeidsforhold",
      "fakta": [
        {
          "id": "9001",
          "type": "boolean",
          "beskrivendeId": "faktum.eos-arbeid-siste-36-mnd",
          "svar": $harEøsarbeidsforhold,
          "roller": [
            "søker"
          ],
          "gyldigeValg": [
            "faktum.eos-arbeid-siste-36-mnd.svar.ja",
            "faktum.eos-arbeid-siste-36-mnd.svar.nei"
          ]
        },
        {
          "id": "9002",
          "type": "generator",
          "beskrivendeId": "faktum.eos-arbeidsforhold",
          "svar": [
            [
              {
                "id": "9003.1",
                "type": "tekst",
                "beskrivendeId": "faktum.eos-arbeidsforhold.arbeidsgivernavn",
                "svar": "CERN",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "9004.1",
                "type": "land",
                "beskrivendeId": "faktum.eos-arbeidsforhold.land",
                "svar": "CHE",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "9005.1",
                "type": "tekst",
                "beskrivendeId": "faktum.eos-arbeidsforhold.personnummer",
                "svar": "12345678901",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "9006.1",
                "type": "periode",
                "beskrivendeId": "faktum.eos-arbeidsforhold.varighet",
                "svar": {
                  "fom": "2022-04-12",
                  "tom": "2022-06-01"
                },
                "roller": [
                  "søker"
                ]
              }
            ],
            [
              {
                "id": "9003.2",
                "type": "tekst",
                "beskrivendeId": "faktum.eos-arbeidsforhold.arbeidsgivernavn",
                "svar": "CERN",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "9004.2",
                "type": "land",
                "beskrivendeId": "faktum.eos-arbeidsforhold.land",
                "svar": "CHE",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "9005.2",
                "type": "tekst",
                "beskrivendeId": "faktum.eos-arbeidsforhold.personnummer",
                "svar": "12345678901",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "9006.2",
                "type": "periode",
                "beskrivendeId": "faktum.eos-arbeidsforhold.varighet",
                "svar": {
                  "fom": "2022-04-12",
                  "tom": "2022-06-01"
                },
                "roller": [
                  "søker"
                ]
              }
            ]
          ],
          "roller": [
            "søker"
          ],
          "templates": [
            {
              "id": "9003",
              "type": "tekst",
              "beskrivendeId": "faktum.eos-arbeidsforhold.arbeidsgivernavn",
              "roller": [
                "søker"
              ]
            },
            {
              "id": "9004",
              "type": "land",
              "beskrivendeId": "faktum.eos-arbeidsforhold.land",
              "roller": [
                "søker"
              ]
            },
            {
              "id": "9005",
              "type": "tekst",
              "beskrivendeId": "faktum.eos-arbeidsforhold.personnummer",
              "roller": [
                "søker"
              ]
            },
            {
              "id": "9006",
              "type": "periode",
              "beskrivendeId": "faktum.eos-arbeidsforhold.varighet",
              "roller": [
                "søker"
              ]
            }
          ]
        }
      ]
    }
  ]
}""".toJsonNode()

@Language("JSON")
internal fun avsluttedeArbeidsforholdQuizJson(
    permitterterFraFiskeForedling: Boolean = false,
    konkurs: Boolean = false,
    permittert: Boolean = false
) =
    """
{
  "seksjoner": [
    {
      "beskrivendeId": "din-situasjon",
      "fakta": [
        {
          "id": "8003",
          "type": "generator",
          "beskrivendeId": "faktum.arbeidsforhold",
          "svar": [
            [
              {
                "id": "8005.1",
                "type": "land",
                "beskrivendeId": "faktum.arbeidsforhold.land",
                "svar": "NOR",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "8006.1",
                "type": "envalg",
                "beskrivendeId": "faktum.arbeidsforhold.endret",
                "svar": "${årsak(permittert, konkurs)}",
                "roller": [
                  "søker"
                ],
                "gyldigeValg": [
                  "faktum.arbeidsforhold.endret.svar.ikke-endret",
                  "faktum.arbeidsforhold.endret.svar.avskjediget",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver",
                  "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs",
                  "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-selv",
                  "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid",
                  "faktum.arbeidsforhold.endret.svar.permittert"
                ]
              },
              {
                "id": "8015.1",
                "type": "boolean",
                "beskrivendeId": "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering",
                "svar": $permitterterFraFiskeForedling,
                "roller": [
                  "søker"
                ],
                "gyldigeValg": [
                  "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering.svar.ja",
                  "faktum.arbeidsforhold.permittertert-fra-fiskeri-naering.svar.nei"
                ]
              }
            ],
            [
              {
                "id": "8005.1",
                "type": "land",
                "beskrivendeId": "faktum.arbeidsforhold.land",
                "svar": "NOR",
                "roller": [
                  "søker"
                ]
              },
              {
                "id": "8006.1",
                "type": "envalg",
                "beskrivendeId": "faktum.arbeidsforhold.endret",
                "svar": "${årsak(konkurs, permittert)}",
                "roller": [
                  "søker"
                ],
                "gyldigeValg": [
                  "faktum.arbeidsforhold.endret.svar.ikke-endret",
                  "faktum.arbeidsforhold.endret.svar.avskjediget",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-av-arbeidsgiver",
                  "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs",
                  "faktum.arbeidsforhold.endret.svar.kontrakt-utgaatt",
                  "faktum.arbeidsforhold.endret.svar.sagt-opp-selv",
                  "faktum.arbeidsforhold.endret.svar.redusert-arbeidstid",
                  "faktum.arbeidsforhold.endret.svar.permittert"
                ]
              }
            ]]
        }
      ]
    }
  ]
}""".toJsonNode()

private fun årsak(permittert: Boolean, konkurs: Boolean): String {
    return if (permittert) {
        "faktum.arbeidsforhold.endret.svar.permittert"
    } else if (konkurs) {
        "faktum.arbeidsforhold.endret.svar.arbeidsgiver-konkurs"
    } else {
        "faktum.arbeidsforhold.endret.svar.ikke-endret"
    }
}
