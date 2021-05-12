package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.journalpostId
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import no.nav.dagpenger.mottak.helpers.assertDeepEquals
import no.nav.dagpenger.mottak.serder.InnsendingData
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InnsendingPostgresRepositoryTest {

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

    @Test
    fun `håndterer ny lagring av aktivitetslogg`() {
        val innsending = innsendingData.createInnsending()
        val nyLogg = innsendingData.aktivitetslogg.aktiviteter.toMutableList().also {
            it.add(
                InnsendingData.AktivitetsloggData.AktivitetData(
                    alvorlighetsgrad = InnsendingData.AktivitetsloggData.Alvorlighetsgrad.INFO,
                    label = 'N',
                    melding = "NY TEST",
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
        }
        val innsending2 = innsendingData.copy(
            aktivitetslogg = InnsendingData.AktivitetsloggData(nyLogg.toList()),
            tilstand = InnsendingData.TilstandData(
                InnsendingData.TilstandData.InnsendingTilstandTypeData.AvventerFerdigstillJournalpostType,
            )
        ).createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresTestHelper.dataSource)) {
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
            with(InnsendingPostgresRepository(PostgresTestHelper.dataSource)) {
                lagre(innsending)
                lagre(innsending).also {
                    assertAntallRader("soknad_v1", 1)
                    assertAntallRader("innsending_oppfyller_minsteinntekt_v1", 1)
                    assertAntallRader("innsending_eksisterende_arena_saker_v1", 1)
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
            with(InnsendingPostgresRepository(PostgresTestHelper.dataSource)) {
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

    @Test
    fun `Lagring der arena sak er null`() {
        val innsending = innsendingData.copy(arenaSakData = InnsendingData.ArenaSakData(oppgaveId = null, fagsakId = "2234")).createInnsending()
        withMigratedDb {
            with(InnsendingPostgresRepository(PostgresTestHelper.dataSource)) {
                lagre(innsending).also {
                    assertTrue(it > 0, "lagring av innsending feilet")
                }

                hent(innsending.journalpostId()).also {
                    assertDeepEquals(innsending, it)
                }
            }
        }
    }

    private fun assertAntallRader(tabell: String, anntallRader: Int) {
        val faktiskeRader = using(sessionOf(PostgresTestHelper.dataSource)) { session ->
            session.run(
                queryOf("select count(1) from $tabell").map { row ->
                    row.int(1)
                }.asSingle
            )
        }
        assertEquals(anntallRader, faktiskeRader, "Feil anntal rader for tabell: $tabell")
    }
}
