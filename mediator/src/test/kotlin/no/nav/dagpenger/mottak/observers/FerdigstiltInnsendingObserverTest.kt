package no.nav.dagpenger.mottak.observers

import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingObserver.Type.NySøknad
import no.nav.dagpenger.mottak.behov.JsonMapper
import org.apache.kafka.clients.producer.MockProducer
import org.apache.kafka.common.serialization.StringSerializer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class FerdigstiltInnsendingObserverTest {
    private val journalpostId = "12455"

    @Test
    fun `skal sende til melding til journalforing v1 topic på ferdigstilte innsendinger `() {
        val mockProducer = MockProducer(true, StringSerializer(), StringSerializer())
        val observer = FerdigstiltInnsendingObserver(mockProducer)

        observer.innsendingFerdigstilt(ferdigstiltEvent())

        assertEquals(1, mockProducer.history().size)
        val record = mockProducer.history().first()
        assertEquals(journalpostId, record.key())
        val message = JsonMapper.jacksonJsonAdapter.readTree(record.value())
        assertEquals("innsending_ferdigstilt", message["@event_name"].asText())
        assertNotNull(message["@id"].asText())
        assertNotNull(message["@opprettet"].asText())
        assertEquals(journalpostId, message["journalpostId"].asText())
        assertNotNull(message["aktørId"].asText())
        assertNotNull(message["fødselsnummer"].asText())
        assertNotNull(message["datoRegistrert"].asText())
        assertNotNull(message["søknadsData"].asText())
        assertNotNull(message["fagsakId"].asText())
    }

    @Test
    fun `skal sende til melding til journalforing v1 topic på mottatte innsendinger `() {
        val mockProducer = MockProducer(true, StringSerializer(), StringSerializer())
        val observer = FerdigstiltInnsendingObserver(mockProducer)

        observer.innsendingMottatt(ferdigstiltEvent())

        assertEquals(1, mockProducer.history().size)
        val record = mockProducer.history().first()
        assertEquals(journalpostId, record.key())
        val message = JsonMapper.jacksonJsonAdapter.readTree(record.value())
        assertEquals("innsending_mottatt", message["@event_name"].asText())
        assertNotNull(message["@id"].asText())
        assertNotNull(message["@opprettet"].asText())
        assertEquals(journalpostId, message["journalpostId"].asText())
        assertNotNull(message["aktørId"].asText())
        assertNotNull(message["fødselsnummer"].asText())
        assertNotNull(message["datoRegistrert"].asText())
        assertNotNull(message["søknadsData"].asText())
        assertNotNull(message["fagsakId"].asText())
    }

    @Test
    fun `melding til journalforing v1 topic på ferdigstilte innsendinger der person er ukjent`() {
        val mockProducer = MockProducer(true, StringSerializer(), StringSerializer())
        val observer = FerdigstiltInnsendingObserver(mockProducer)

        observer.innsendingFerdigstilt(ukjentPersonEvent())
        assertEquals(1, mockProducer.history().size)
        val record = mockProducer.history().first()

        assertEquals(journalpostId, record.key())
        val message = JsonMapper.jacksonJsonAdapter.readTree(record.value())
        assertEquals("innsending_ferdigstilt", message["@event_name"].asText())
        assertNotNull(message["@id"].asText())
        assertNotNull(message["@opprettet"].asText())
        assertEquals(journalpostId, message["journalpostId"].asText())
        assertNotNull(message["datoRegistrert"].asText())

        assertFalse(message.has("aktørId"))
        assertFalse(message.has("fødselsnummer"))
        assertFalse(message.has("søknadsData"))
        assertFalse(message.has("fagsakId"))
    }

    private fun ukjentPersonEvent(): InnsendingObserver.InnsendingEvent = ferdigstiltEvent().copy(
        fødselsnummer = null,
        aktørId = null,
        søknadsData = null,
        fagsakId = null
    )

    private fun ferdigstiltEvent(): InnsendingObserver.InnsendingEvent =
        InnsendingObserver.InnsendingEvent(
            type = NySøknad,
            journalpostId = journalpostId,
            aktørId = "1234455",
            fødselsnummer = "12345678901",
            fagsakId = "1234",
            datoRegistrert = LocalDateTime.now(),
            søknadsData = JsonMapper.jacksonJsonAdapter.createObjectNode().also {
                it.put("test", "test")
            },
            behandlendeEnhet = "Tadda",
            oppfyllerMinsteinntektArbeidsinntekt = false
        )
}
