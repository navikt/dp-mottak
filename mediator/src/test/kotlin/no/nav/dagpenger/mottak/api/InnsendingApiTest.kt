package no.nav.dagpenger.mottak.api

import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.header
import io.ktor.client.request.put
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.testApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.mottak.db.InnsendingRepository
import no.nav.dagpenger.mottak.observers.FerdigstiltInnsendingObserver
import no.nav.dagpenger.mottak.serder.InnsendingData
import no.nav.dagpenger.mottak.serder.InnsendingData.TilstandData.InnsendingTilstandTypeData.InnsendingFerdigstiltType
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.Base64

internal class InnsendingApiTest {

    val okCredential = Pair("super", "secret")

    @Test
    fun `skal kunne sende ferdigstilt_event`() {
        val journalpostId = "187689"

        val mockProducer = MockProducer(true, StringSerializer(), StringSerializer())
        val innsending = innsendingData.copy(
            tilstand = InnsendingData.TilstandData(
                InnsendingFerdigstiltType
            )
        ).createInnsending()

        val innsendingRepository = mockk<InnsendingRepository>().also {
            every { it.hent(journalpostId) } returns innsending
        }
        val ferdigstiltInnsendingObserver = FerdigstiltInnsendingObserver(mockProducer)
        testApplication {
            application(innsendingApi(innsendingRepository, ferdigstiltInnsendingObserver, okCredential))
            client.put("/internal/replay/187689") {
                withAuth()
            }.let { response ->
                assertEquals(HttpStatusCode.OK, response.status)
                verify(exactly = 1) { innsendingRepository.hent(journalpostId) }
                assertEquals(1, mockProducer.history().size)
            }
        }
    }

    @Test
    fun `skal ikke kunne sende ferdigstilt_event hvis status annen enn ferdigstill`() {
        val journalpostId = "187689"

        val mockProducer = MockProducer(true, StringSerializer(), StringSerializer())
        val innsending = innsendingData.createInnsending()

        val innsendingRepository = mockk<InnsendingRepository>().also {
            every { it.hent(journalpostId) } returns innsending
        }
        val ferdigstiltInnsendingObserver = FerdigstiltInnsendingObserver(mockProducer)

        testApplication {
            application(innsendingApi(innsendingRepository, ferdigstiltInnsendingObserver, okCredential))
            client.put("/internal/replay/187689") {
                withAuth()
            }.let { response ->
                assertEquals(HttpStatusCode.BadRequest, response.status)
                verify(exactly = 1) { innsendingRepository.hent(journalpostId) }
                assertEquals(0, mockProducer.history().size)
            }
        }
    }

    private fun HttpRequestBuilder.withAuth() {
        val up = "${okCredential.first}:${okCredential.second}"
        val encoded = String(Base64.getEncoder().encode(up.toByteArray(Charsets.ISO_8859_1)))
        header(HttpHeaders.Authorization, "Basic $encoded")
    }
}
