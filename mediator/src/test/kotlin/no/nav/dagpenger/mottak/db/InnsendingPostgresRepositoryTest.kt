package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.dnr
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.journalpostId
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingVisitor
import no.nav.dagpenger.mottak.api.Periode
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import no.nav.dagpenger.mottak.helpers.assertDeepEquals
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.Journalpost.DokumentInfo.Companion.hovedDokument
import no.nav.dagpenger.mottak.serder.InnsendingData
import org.junit.Ignore
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InnsendingPostgresRepositoryTest {
    class TestVisitor(innsending: Innsending) : InnsendingVisitor {
        val forventetDokumenter = mutableListOf<Journalpost.DokumentInfo>()

        init {
            innsending.accept(this)
        }

        override fun visitJournalpost(
            journalpostId: String,
            journalpostStatus: String,
            bruker: Journalpost.Bruker?,
            behandlingstema: String?,
            registrertDato: LocalDateTime,
            dokumenter: List<Journalpost.DokumentInfo>,
        ) {
            forventetDokumenter.addAll(dokumenter)
        }
    }

    @Test
    fun `hent skal kunne hente innsending`() {
        val innsending = innsendingData.createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)) {
                lagre(innsending).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                hent(journalpostId).also {
                    assertDeepEquals(innsending, it)
                    assertEquals("NAV 04-01.03", TestVisitor(it!!).forventetDokumenter.hovedDokument().brevkode)
                }
            }
        }
    }

    @Test
    fun `håndterer ny lagring av aktivitetslogg`() {
        val innsending = innsendingData.createInnsending()
        val nyLogg =
            innsendingData.aktivitetslogg.aktiviteter.toMutableList().also {
                it.add(
                    InnsendingData.AktivitetsloggData.AktivitetData(
                        alvorlighetsgrad = InnsendingData.AktivitetsloggData.Alvorlighetsgrad.INFO,
                        label = 'N',
                        melding = "NY TEST",
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
                )
            }
        val innsending2 =
            innsendingData.copy(
                aktivitetslogg = InnsendingData.AktivitetsloggData(nyLogg.toList()),
                tilstand =
                    InnsendingData.TilstandData(
                        InnsendingData.TilstandData.InnsendingTilstandTypeData.AvventerFerdigstillJournalpostType,
                    ),
            ).createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)) {
                lagre(innsending).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                lagre(innsending2).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                hent(innsending.journalpostId()).also {
                    assertDeepEquals(it, innsending2)
                }
            }
        }
    }

    @Test
    fun `Dobbelagrer ikke verdier som skal være unike`() {
        val innsending = innsendingData.createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)) {
                lagre(innsending)
                lagre(innsending).also {
                    assertAntallRader("soknad_v1", 1)
                    assertAntallRader("person_innsending_v1", 1)
                    assertAntallRader("aktivitetslogg_v1", 1)
                    assertAntallRader("arenasak_v1", 1)
                    assertAntallRader("journalpost_v1", 1)
                    assertAntallRader("journalpost_dokumenter_v1", 3)
                    assertAntallRader("person_v1", 1)
                    assertAntallRader("person_innsending_v1", 1)
                }
            }
        }
    }

    @Test
    fun `håndterer flere innsendinger for samme person`() {
        val innsending = innsendingData.createInnsending()
        val innsending2 = innsendingData.copy(journalpostId = "287689").createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)) {
                lagre(innsending).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                lagre(innsending2).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                assertAntallRader("person_v1", 1)
                assertAntallRader("person_innsending_v1", 2)
                assertAntallRader("innsending_v1", 2)
                assertAntallRader("soknad_v1", 2)
            }
        }
    }

    @Test @Ignore
    fun `håndterer flere innsendinger for samme person men med dnr og fnr skille`() {
        val innsending = innsendingData.createInnsending()

        val innsending2 =
            innsendingData.copy(
                journalpostId = "287689",
                personData =
                    innsendingData.personData!!.copy(
                        fødselsnummer = dnr,
                    ),
            ).createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)) {
                lagre(innsending).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                lagre(innsending2).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                assertAntallRader("person_v1", 2)
                assertAntallRader("person_innsending_v1", 2)
                assertAntallRader("innsending_v1", 2)
                assertAntallRader("soknad_v1", 2)
            }
        }
    }

    @Test
    fun `Lagring der arena sak er null`() {
        val innsending =
            innsendingData.copy(
                arenaSakData =
                    InnsendingData.ArenaSakData(
                        oppgaveId = "2234",
                        fagsakId = null,
                    ),
            ).createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)) {
                lagre(innsending).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                hent(innsending.journalpostId()).also {
                    assertDeepEquals(innsending, it)
                }
            }
        }
    }

    @Test
    fun `Skal kunne hente innsedninger for en gitt periode`() {
        val innsending = innsendingData.createInnsending()

        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)) {
                lagre(innsending)
                val innsendinger = forPeriode(Periode(LocalDateTime.now().minusDays(1), LocalDateTime.now().plusMinutes(1)))
                assertFalse(innsendinger.isEmpty())
            }
        }
    }

    private fun assertAntallRader(
        tabell: String,
        antallRader: Int,
    ) {
        val faktiskeRader =
            using(sessionOf(PostgresDataSourceBuilder.dataSource)) { session ->
                session.run(
                    queryOf("select count(1) from $tabell").map { row ->
                        row.int(1)
                    }.asSingle,
                )
            }
        assertEquals(antallRader, faktiskeRader, "Feil antall rader for tabell: $tabell")
    }
}
