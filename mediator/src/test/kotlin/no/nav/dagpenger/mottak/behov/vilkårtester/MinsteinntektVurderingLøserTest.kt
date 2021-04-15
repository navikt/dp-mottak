package no.nav.dagpenger.mottak.behov.vilkårtester

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.lang.RuntimeException
import java.time.LocalDateTime
import java.util.UUID

internal class MinsteinntektVurderingLøserTest {

    private val journalpostId = "1234567"
    private val aktørId = "456"
    val testRapid = TestRapid()
    val regelApiClientMock = mockk<RegelApiClient>(relaxed = true)
    init {
        MinsteinntektVurderingLøser(testRapid, regelApiClientMock)
    }

    @Test
    fun `Starter minsteinntektvurdering`() {
        testRapid.sendTestMessage(minsteinntektBehov())
        verify(exactly = 1) {
            regelApiClientMock.startMinsteinntektVurdering(journalpostId = journalpostId, aktørId = aktørId)
        }
    }

    @Test
    fun `Starter minsteinntektvurdering og svarer med null hvis dp-regel-api er nede`() {
        every {
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
}
