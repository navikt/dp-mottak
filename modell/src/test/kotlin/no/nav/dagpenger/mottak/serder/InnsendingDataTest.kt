package no.nav.dagpenger.mottak.serder

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
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
import java.time.Duration
import java.time.ZonedDateTime

internal class InnsendingDataTest {
    private val journalpostId = "ertyu"
    private val journalpostStatus = "aktiv"
    private val fnr = "12345678910"
    private val registrertdato = ZonedDateTime.now()
    private val dokumenter = listOf<DokumentInfoData>(
        DokumentInfoData(
            tittel = "Fin tittel",
            brevkode = "NAV 04-01.03",
            dokumentInfoId = "12345678"
        )
    )

    @Test
    fun `Skal lage innsending`() {
        val innsendingData = InnsendingData(
            journalpostId = journalpostId,
            tilstand = TilstandData(
                TilstandData.InnsendingTilstandTypeData.AventerArenaOppgaveType,
                Duration.ofDays(1)
            ),
            journalpostData = JournalpostData(
                journalpostId = journalpostId,
                journalpostStatus = journalpostStatus,
                bruker = BrukerData(BrukerTypeData.FNR, fnr),
                behandlingstema = "DAG",
                registertDato = registrertdato,
                dokumenter = dokumenter
            ),
            oppfyllerMinsteArbeidsinntekt = true,
            eksisterendeSaker = false,
            arenaSakData = ArenaSakData(
                oppgaveId = "123487",
                fagsakId = "129678"
            ),
            søknadsData = søknadsjson,
            aktivitetslogg = AktivitetsloggData(emptyList()),
            personData = PersonData(
                fødselsnummer = fnr,
                aktørId = "345678",
                norskTilknytning = true,
                // TODO: wattudu med diskresjonskoder(egen ansatt, 6 og 7)
                diskresjonskode = false
            )
        )

        innsendingData.createInnsending().also {
            assertEquals(journalpostId, it.journalpostId())
        }
    }

    //language=JSON
    private val søknadsjson = jacksonObjectMapper().readTree(
        """
            {
            "tadda":"it´s short for"
            }
        """.trimIndent()
    )
}
