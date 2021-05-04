package no.nav.dagpenger.mottak.behov.vilkårtester

import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.dagpenger.mottak.db.MinsteinntektVurderingPostgresRepository
import no.nav.dagpenger.mottak.db.PostgresTestHelper
import no.nav.dagpenger.mottak.db.runMigration
import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class MinsteinntektVurderingLøserTest {

    private val journalpostId = "1234567"
    private val aktørId = "456"
    val testRapid = TestRapid()
    val regelApiClientMock = mockk<RegelApiClient>(relaxed = true)
    private val packetRepository = mutableMapOf<String, JsonMessage>()
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
            assertTrue(field(0, "@løsning")["MinsteinntektVurdering"].isNull)
        }
    }

    private fun minsteinntektBehov(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
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
