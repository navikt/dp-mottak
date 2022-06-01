package no.nav.dagpenger.mottak.behov.vilkårtester

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.db.MinsteinntektVurderingPostgresRepository
import no.nav.dagpenger.mottak.db.PostgresTestHelper
import no.nav.dagpenger.mottak.db.runMigration
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageProblems
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import java.util.UUID

internal class MinsteinntektVurderingLøserTest {

    private val journalpostId = "1234567"
    private val aktørId = "456"
    val testRapid = TestRapid()
    val regelApiClientMock = mockk<RegelApiClient>(relaxed = true)
    private val minsteinntektVurderingRepository =
        MinsteinntektVurderingPostgresRepository(dataSource = PostgresTestHelper.dataSource).also {
            runMigration(PostgresTestHelper.dataSource)
        }

    init {
        MinsteinntektVurderingLøser(
            regelApiClient = regelApiClientMock,
            repository = minsteinntektVurderingRepository,
            rapidsConnection = testRapid
        )
    }

    @Test
    fun `Løser minsteinntekt vurdering behov`() {
        testRapid.sendTestMessage(minsteinntektBehov())
        coVerify(exactly = 1) {
            regelApiClientMock.startMinsteinntektVurdering(journalpostId = journalpostId, aktørId = aktørId)
        }
        testRapid.sendTestMessage(minsteinntektLøsningMessage())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertTrue(field(0, "@løsning")["MinsteinntektVurdering"]["oppfyllerMinsteArbeidsinntekt"].asBoolean())
        }
    }

    @Test
    fun `Starter minsteinntektvurdering og svarer med null hvis dp-regel-api er nede`() {
        coEvery {
            regelApiClientMock.startMinsteinntektVurdering(journalpostId = journalpostId, aktørId = aktørId)
        } throws RuntimeException()

        testRapid.sendTestMessage(minsteinntektBehov())
        with(testRapid.inspektør) {
            assertEquals(1, size)
            assertTrue(field(0, "@løsning")["MinsteinntektVurdering"]["oppfyllerMinsteArbeidsinntekt"].isNull)
        }
    }

    @Test
    fun `Vaktermester skal rydde`() {
        runBlocking {
            val repository = mockk<MinsteinntektVurderingRepository>().also {
                every { it.slettUtgåtteVurderinger() } returns listOf(
                    Pair("0", JsonMessage("""{}""", MessageProblems(""))),
                    Pair("1", JsonMessage("""{}""", MessageProblems("")))
                ) andThen emptyList()
            }
            MinsteinntektVurderingLøser(
                oppryddningPeriode = 100.toLong(),
                regelApiClient = mockk(),
                repository = repository,
                rapidsConnection = testRapid
            )
            delay(500)

            assertEquals(2, testRapid.inspektør.size)
            assertTrue(testRapid.inspektør.message(0)["@løsning"]["MinsteinntektVurdering"]["oppfyllerMinsteArbeidsinntekt"].isNull)
            assertTrue(testRapid.inspektør.message(1)["@løsning"]["MinsteinntektVurdering"]["oppfyllerMinsteArbeidsinntekt"].isNull)
            assertEquals("0", testRapid.inspektør.key(0))
            assertEquals("1", testRapid.inspektør.key(1))
        }
    }

    @Test
    fun `Kun en minsteinntekt opprydder skal kunne kjøre samtidig`() {
        runBlocking {
            using(sessionOf(PostgresTestHelper.dataSource)) { session ->
                session.run(
                    queryOf(
                        "INSERT INTO  minsteinntekt_vurdering_v1(journalpostId,packet, opprettet) VALUES(:journalpostId,:packet, :opprettet) ON CONFLICT DO NOTHING",
                        mapOf(
                            "journalpostId" to 12345,
                            "opprettet" to LocalDateTime.now().minusDays(2),
                            "packet" to PGobject().apply {
                                type = "jsonb"
                                value = JsonMessage("""{}""", MessageProblems("")).toJson()
                            }
                        )
                    ).asUpdate
                )

                MinsteinntektVurderingLøser(
                    oppryddningPeriode = 400.toLong(),
                    regelApiClient = mockk(),
                    repository = minsteinntektVurderingRepository,
                    rapidsConnection = testRapid
                )
                MinsteinntektVurderingLøser(
                    oppryddningPeriode = 401.toLong(),
                    regelApiClient = mockk(),
                    repository = minsteinntektVurderingRepository,
                    rapidsConnection = testRapid
                )
                MinsteinntektVurderingLøser(
                    oppryddningPeriode = 402.toLong(),
                    regelApiClient = mockk(),
                    repository = minsteinntektVurderingRepository,
                    rapidsConnection = testRapid
                )
            }

            delay(500)

            assertEquals(1, testRapid.inspektør.size)
        }
    }

    private fun minsteinntektBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behovId": "${UUID.randomUUID()}",
          "@behov": [
            "MinsteinntektVurdering"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": $journalpostId,
          "aktørId": $aktørId
        }
        """.trimIndent()

    //language=JSON
    private fun minsteinntektLøsningMessage(): String =
        """{
              "system_read_count": 5,
              "system_started": "2021-04-16T13:05:11.39579",
              "system_correlation_id": "01F3D5PAZ3NZZFE80QRX751ENK",
              "behovId": "01F3D5PAYS0BQ6CKNBABMF3ESF",
              "aktørId": "1000096233942",
              "kontekstId": $journalpostId,
              "kontekstType": "soknad",
              "behandlingsId": "01F3D5EG005PYBB780VM233GQH",
              "beregningsDato": "2021-04-16",
              "antallBarn": 0.0,
              "regelverksdato": "2021-04-16",
              "minsteinntektResultat": {
                "sporingsId": "01F3D5PB56XK1E0HSHFQ3QYB7M",
                "subsumsjonsId": "01F3D5PB56KQTYX38T287QN9JV",
                "regelIdentifikator": "Minsteinntekt.v1",
                "oppfyllerMinsteinntekt": true,
                "beregningsregel": "KORONA"
              }
            }"""
}
