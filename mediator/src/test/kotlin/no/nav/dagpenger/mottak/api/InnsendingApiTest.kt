package no.nav.dagpenger.mottak.api

import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import io.ktor.client.request.get
import io.ktor.client.request.put
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.innsendingData
import no.nav.dagpenger.mottak.InnsendingPeriode
import no.nav.dagpenger.mottak.api.TestApplication.autentisert
import no.nav.dagpenger.mottak.api.TestApplication.withMockAuthServerAndTestApplication
import no.nav.dagpenger.mottak.db.InnsendingRepository
import no.nav.dagpenger.mottak.observers.FerdigstiltInnsendingObserver
import no.nav.dagpenger.mottak.serder.InnsendingData
import no.nav.dagpenger.mottak.serder.InnsendingData.TilstandData.InnsendingTilstandTypeData.InnsendingFerdigstiltType
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.clients.producer.RoundRobinPartitioner
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InnsendingApiTest {
    companion object {
        val objectMapper = jacksonObjectMapper()
    }

    @Test
    fun `skal kunne sende ferdigstilt_event`() {
        val journalpostId = "187689"

        val mockProducer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
        val innsending =
            innsendingData
                .copy(
                    tilstand =
                        InnsendingData.TilstandData(
                            InnsendingFerdigstiltType,
                        ),
                ).createInnsending()

        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.hent(journalpostId) } returns innsending
            }
        val ferdigstiltInnsendingObserver = FerdigstiltInnsendingObserver(mockProducer)
        withMockAuthServerAndTestApplication({ innsendingApi(innsendingRepository, ferdigstiltInnsendingObserver) }) {
            client
                .put("/internal/replay/187689") {
                    autentisert()
                }.let { response ->
                    assertEquals(HttpStatusCode.OK, response.status)
                    assertEquals("text/plain; charset=UTF-8", response.headers[HttpHeaders.ContentType])
                    verify(exactly = 1) { innsendingRepository.hent(journalpostId) }
                    assertEquals(1, mockProducer.history().size)
                }
        }
    }

    @Test
    fun `skal ikke kunne sende ferdigstilt_event hvis status annen enn ferdigstill`() {
        val journalpostId = "187689"

        val mockProducer = MockProducer(true, RoundRobinPartitioner(), StringSerializer(), StringSerializer())
        val innsending = innsendingData.createInnsending()

        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.hent(journalpostId) } returns innsending
            }
        val ferdigstiltInnsendingObserver = FerdigstiltInnsendingObserver(mockProducer)

        withMockAuthServerAndTestApplication({ innsendingApi(innsendingRepository, ferdigstiltInnsendingObserver) }) {
            client
                .put("/internal/replay/187689") {
                    autentisert()
                }.let { response ->
                    assertEquals(HttpStatusCode.BadRequest, response.status)
                    verify(exactly = 1) { innsendingRepository.hent(journalpostId) }
                    assertEquals(0, mockProducer.history().size)
                }
        }
    }

    @Test
    fun `skal kunne hente innsendinger i en gitt periode`() {
        val idag = LocalDateTime.now()
        val innsendingRepository =
            mockk<InnsendingRepository>().also {
                every { it.forPeriode(any()) } returns
                    listOf(
                        InnsendingPeriode(
                            ident = "1234556777",
                            registrertDato = idag,
                            journalpostId = "124433",
                        ),
                    )
            }

        withMockAuthServerAndTestApplication({
            innsendingApi(innsendingRepository, mockk())
        }) {
            client
                .get("/innsending/periode?fom=2020-01-01T21:10&tom=2020-01-01T23:10") {
                    autentisert()
                }.let { response ->
                    assertEquals(HttpStatusCode.OK, response.status)
                    verify(exactly = 1) { innsendingRepository.forPeriode(any()) }
                    assertEquals("application/json; charset=UTF-8", response.headers[HttpHeaders.ContentType])
                    val bodyAsText = response.bodyAsText()
                    val innsendinger = objectMapper.readTree(bodyAsText)
                    assertEquals(1, innsendinger.size())
                    assertEquals("1234556777", innsendinger[0]["ident"].asText())
                    assertEquals(idag, innsendinger[0]["registrertDato"].asLocalDateTime())
                    assertEquals("124433", innsendinger[0]["journalpostId"].asText())
                }
        }
    }
}
