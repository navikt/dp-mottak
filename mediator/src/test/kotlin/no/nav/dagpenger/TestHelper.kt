package no.nav.dagpenger

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_DNUMMER
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import no.nav.dagpenger.mottak.serder.InnsendingData
import java.time.LocalDateTime

internal val journalpostId = "187689"
internal val journalpostStatus = "aktiv"
internal val fnr = GENERERT_FØDSELSNUMMER
internal val dnr = GENERERT_DNUMMER
internal val registrertdato = LocalDateTime.now()
internal val søknadDokumenter =
    listOf(
        InnsendingData.JournalpostData.DokumentInfoData(
            tittel = "Fin tittel",
            brevkode = "NAV 04-01.03",
            dokumentInfoId = "12345678",
            hovedDokument = true,
        ),
        InnsendingData.JournalpostData.DokumentInfoData(
            tittel = "Annen Fin tittel",
            brevkode = "O2",
            dokumentInfoId = "123456567",
            hovedDokument = false,
        ),
        InnsendingData.JournalpostData.DokumentInfoData(
            tittel = "Permitteringsvarsel: Koko's AS",
            brevkode = "T6",
            dokumentInfoId = "12366732",
            hovedDokument = false,
        ),
    )
internal val ettersendingDokumenter =
    listOf(
        InnsendingData.JournalpostData.DokumentInfoData(
            tittel = "Fin tittel",
            brevkode = "NAVe 04-01.03",
            dokumentInfoId = "123455555",
            hovedDokument = true,
        ),
    )

private val aktivitetsloggData =
    InnsendingData.AktivitetsloggData(
        listOf(
            InnsendingData.AktivitetsloggData.AktivitetData(
                alvorlighetsgrad = InnsendingData.AktivitetsloggData.Alvorlighetsgrad.INFO,
                label = 'N',
                melding = "TEST",
                tidsstempel = LocalDateTime.now().toString(),
                detaljer = mapOf("detaljVariabel" to "tt"),
                kontekster =
                    listOf(
                        InnsendingData.AktivitetsloggData.SpesifikkKontekstData(
                            kontekstType = "TEST",
                            kontekstMap = mapOf("kontekstVariabel" to "foo"),
                        ),
                    ),
                behovtype = null,
            ),
        ),
    )

//language=JSON
private val søknadsjson =
    jacksonObjectMapper().readTree(
        """
        {
        "versjon_navn": "Dagpenger",
        "seksjoner":[]
        }
        """.trimIndent(),
    )

val innsendingData =
    InnsendingData(
        id = 1,
        journalpostId = journalpostId,
        tilstand =
            InnsendingData.TilstandData(
                InnsendingData.TilstandData.InnsendingTilstandTypeData.AventerArenaOppgaveType,
            ),
        journalpostData =
            InnsendingData.JournalpostData(
                journalpostId = journalpostId,
                journalpostStatus = journalpostStatus,
                bruker = InnsendingData.JournalpostData.BrukerData(InnsendingData.JournalpostData.BrukerTypeData.FNR, fnr),
                behandlingstema = "DAG",
                registertDato = registrertdato,
                dokumenter = søknadDokumenter,
                journalførendeEnhet = "ENHET",
            ),
        personData =
            InnsendingData.PersonData(
                navn = "Hubba Bubba's",
                fødselsnummer = fnr,
                aktørId = "345678",
                norskTilknytning = true,
                diskresjonskode = false,
                egenAnsatt = false,
            ),
        arenaSakData =
            InnsendingData.ArenaSakData(
                oppgaveId = "123487",
                fagsakId = "129678",
            ),
        søknadsData = søknadsjson,
        aktivitetslogg = aktivitetsloggData,
        mottakskanal = "NAV_NO",
    )
