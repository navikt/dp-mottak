package no.nav.dagpenger.mottak.serder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData
import no.nav.dagpenger.mottak.serder.InnsendingData.ArenaSakData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerTypeData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.DokumentInfoData
import no.nav.dagpenger.mottak.serder.InnsendingData.PersonData
import no.nav.dagpenger.mottak.serder.InnsendingData.TilstandData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InnsendingDataTest {
    private val journalpostId = "ertyu"
    private val journalpostStatus = "aktiv"
    private val fnr = GENERERT_FØDSELSNUMMER
    private val registrertdato = LocalDateTime.now()
    private val dokumenter =
        listOf(
            DokumentInfoData(
                tittel = "Fin tittel",
                brevkode = "NAV 04-01.03",
                dokumentInfoId = "12345678",
                hovedDokument = true,
            ),
        )

    @Test
    fun `Skal lage innsending`() {
        val innsendingData =
            InnsendingData(
                id = 111L,
                journalpostId = journalpostId,
                tilstand =
                    TilstandData(
                        TilstandData.InnsendingTilstandTypeData.AventerArenaOppgaveType,
                    ),
                journalpostData =
                    JournalpostData(
                        journalpostId = journalpostId,
                        journalpostStatus = journalpostStatus,
                        bruker = BrukerData(BrukerTypeData.FNR, fnr),
                        behandlingstema = "DAG",
                        registertDato = registrertdato,
                        dokumenter = dokumenter,
                        journalførendeEnhet = "ENHET",
                    ),
                personData =
                    PersonData(
                        navn = "Hubba Bubba",
                        fødselsnummer = fnr,
                        aktørId = "345678",
                        norskTilknytning = true,
                        diskresjonskode = false,
                        egenAnsatt = false,
                    ),
                arenaSakData =
                    ArenaSakData(
                        oppgaveId = "123487",
                        fagsakId = "129678",
                    ),
                søknadsData = søknadsjson,
                mottakskanal = "NAV_NO",
                aktivitetslogg = AktivitetsloggData(emptyList()),
            )

        innsendingData.createInnsending().also {
            assertEquals(journalpostId, it.journalpostId())
        }
    }

    //language=JSON
    private val søknadsjson =
        jacksonObjectMapper().readTree(
            """
            {
            "tadda":"it´s short for"
            }
            """.trimIndent(),
        )
}
