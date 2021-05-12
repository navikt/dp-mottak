package no.nav.dagpenger.mottak.api

import io.ktor.http.HttpHeaders
import io.ktor.http.HttpMethod
import io.ktor.http.HttpStatusCode
import io.ktor.server.testing.TestApplicationRequest
import io.ktor.server.testing.handleRequest
import io.ktor.server.testing.withTestApplication
import io.ktor.util.InternalAPI
import io.ktor.util.encodeBase64
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

        withTestApplication(innsendingApi(innsendingRepository, ferdigstiltInnsendingObserver, okCredential)) {
            with(this.handleRequest(HttpMethod.Put, "/internal/replay/187689", withAuth())) {
                assertEquals(HttpStatusCode.OK, response.status())
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

        withTestApplication(innsendingApi(innsendingRepository, ferdigstiltInnsendingObserver, okCredential)) {
            with(this.handleRequest(HttpMethod.Put, "/internal/replay/187689", withAuth())) {
                assertEquals(HttpStatusCode.BadRequest, response.status())
                verify(exactly = 1) { innsendingRepository.hent(journalpostId) }
                assertEquals(0, mockProducer.history().size)
            }
        }
    }

    @OptIn(InternalAPI::class)
    private fun withAuth(): TestApplicationRequest.() -> Unit = {
        val up = "${okCredential.first}:${okCredential.second}"
        val encoded = up.toByteArray(Charsets.ISO_8859_1).encodeBase64()
        addHeader(HttpHeaders.Authorization, "Basic $encoded")
    }
}
