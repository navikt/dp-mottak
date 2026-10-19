package no.nav.dagpenger.mottak.serder

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import no.nav.dagpenger.mottak.defaultObjectMapper
import no.nav.dagpenger.mottak.e2e.TestObservatør
import no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
import no.nav.dagpenger.mottak.serder.InnsendingData.AktivitetsloggData
import no.nav.dagpenger.mottak.serder.InnsendingData.ArenaSakData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerTypeData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.DokumentInfoData
import no.nav.dagpenger.mottak.serder.InnsendingData.PersonData
import no.nav.dagpenger.mottak.serder.InnsendingData.TilstandData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

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

    @Test
    fun `innsending med både Arena-sak og Dagpenger-sak ferdigstilles som Dagpenger`() {
        val sakId = UUID.randomUUID()
        val innsendingData =
            InnsendingData(
                id = 111L,
                journalpostId = journalpostId,
                tilstand =
                    TilstandData(
                        TilstandData.InnsendingTilstandTypeData.AvventerFerdigstillJournalpostType,
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
                sakId = sakId,
                søknadsData = søknadsjson,
                mottakskanal = "NAV_NO",
                aktivitetslogg = AktivitetsloggData(emptyList()),
            )
        val observatør = TestObservatør()

        innsendingData.createInnsending().also {
            it.addObserver(observatør)
            it.håndter(JournalpostFerdigstilt(Aktivitetslogg(), journalpostId))
        }

        val event = requireNotNull(observatør.event) { "Forventet innsending_ferdigstilt" }
        assertEquals(Fagsystem.FagsystemType.DAGPENGER, event.fagsystem)
        assertEquals(sakId.toString(), event.fagsakId)
        assertNull(event.oppgaveId)
    }

    //language=JSON
    private val søknadsjson =
        defaultObjectMapper.readTree(
            """
            {
            "tadda":"it´s short for"
            }
            """.trimIndent(),
        )
}
