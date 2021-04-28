package no.nav.dagpenger.mottak.db

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import no.nav.dagpenger.mottak.helpers.assertDeepEquals
import no.nav.dagpenger.mottak.serder.InnsendingData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.DokumentInfoData
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InnsendingPostgresRepositoryTest {

    private val journalpostId = "187689"
    private val journalpostStatus = "aktiv"
    private val fnr = "12345678910"
    private val registrertdato = LocalDateTime.now()
    private val dokumenter = listOf(
        DokumentInfoData(
            tittel = "Fin tittel",
            brevkode = "NAV 04-01.03",
            dokumentInfoId = "12345678"
        ),
        DokumentInfoData(
            tittel = "Annen Fin tittel",
            brevkode = "NAV 04-01.03",
            dokumentInfoId = "123456567"
        )
    )

    private val aktivitetsloggData = InnsendingData.AktivitetsloggData(
        listOf(
            InnsendingData.AktivitetsloggData.AktivitetData(
                alvorlighetsgrad = InnsendingData.AktivitetsloggData.Alvorlighetsgrad.INFO,
                label = 'N',
                melding = "TEST",
                tidsstempel = LocalDateTime.now().toString(),
                detaljer = mapOf("detaljVariabel" to "tt"),
                kontekster = listOf(
                    InnsendingData.AktivitetsloggData.SpesifikkKontekstData(
                        kontekstType = "TEST",
                        kontekstMap = mapOf("kontekstVariabel" to "foo")
                    )
                ),
                behovtype = null
            )
        )

    )
    //language=JSON
    private val søknadsjson = jacksonObjectMapper().readTree(
        """
            {
            "tadda":"it´s short for"
            }
        """.trimIndent()
    )

    val innsendingData = InnsendingData(
        id = 1,
        journalpostId = journalpostId,
        tilstand = InnsendingData.TilstandData(
            InnsendingData.TilstandData.InnsendingTilstandTypeData.AventerArenaOppgaveType,
        ),
        journalpostData = InnsendingData.JournalpostData(
            journalpostId = journalpostId,
            journalpostStatus = journalpostStatus,
            bruker = InnsendingData.JournalpostData.BrukerData(InnsendingData.JournalpostData.BrukerTypeData.FNR, fnr),
            behandlingstema = "DAG",
            registertDato = registrertdato,
            dokumenter = dokumenter
        ),
        oppfyllerMinsteArbeidsinntekt = true,
        eksisterendeSaker = false,
        personData = InnsendingData.PersonData(
            fødselsnummer = fnr,
            aktørId = "345678",
            norskTilknytning = true,
            // TODO: wattudu med diskresjonskoder(egen ansatt, 6 og 7)
            diskresjonskode = false
        ),
        arenaSakData = InnsendingData.ArenaSakData(
            oppgaveId = "123487",
            fagsakId = "129678"
        ),
        søknadsData = søknadsjson,
        aktivitetslogg = aktivitetsloggData
    )

    @Test
    fun `hent skal kunne hente innsending`() {

        val innsending = innsendingData.createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresTestHelper.dataSource)) {
                lagre(innsending).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                hent(journalpostId).also {
                    assertDeepEquals(innsending, it)
                }
            }
        }
    }
}
