package no.nav.dagpenger.mottak.observers

import mu.KotlinLogging
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.helse.rapids_rivers.JsonMessage
import org.apache.kafka.clients.producer.KafkaProducer
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.clients.producer.ProducerRecord
import org.apache.kafka.common.serialization.StringSerializer
import java.time.LocalDateTime
import java.util.Properties
import java.util.UUID
import java.util.concurrent.TimeUnit

internal class FerdigstiltInnsendingObserver internal constructor(private val producer: Producer<String, String>) :
    InnsendingObserver {
    constructor(producerProperties: Properties) : this(createProducer(producerProperties))

    init {
        Runtime.getRuntime().addShutdownHook(Thread(::shutdownHook))
    }

    private companion object {
        private val logger = KotlinLogging.logger {}
        private val sikkerLogger = KotlinLogging.logger("tjenestekall")
        private fun createProducer(producerProperties: Properties): KafkaProducer<String, String> {
            producerProperties[ProducerConfig.ACKS_CONFIG] = "all"
            producerProperties[ProducerConfig.LINGER_MS_CONFIG] = "0"
            producerProperties[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "1"
            return KafkaProducer(producerProperties, StringSerializer(), StringSerializer())
        }
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        val payload = event.toPayload().also {
            it["@event_name"] = "innsending_ferdigstilt"
        }

        publish(
            event.journalpostId,
            JsonMessage.newMessage(
                payload
            )
        )
    }

    override fun innsendingMottatt(event: InnsendingObserver.InnsendingEvent) {
        val payload = event.toPayload().also {
            it["@event_name"] = "innsending_mottatt"
        }

        publish(
            event.journalpostId,
            JsonMessage.newMessage(
                payload
            )
        )
    }

    private fun publish(
        key: String,
        message: JsonMessage
    ) {
        producer.send(
            ProducerRecord(
                "teamdagpenger.journalforing.v1",
                key,
                message.toJson()
            )
        ).get(500, TimeUnit.MILLISECONDS)

        logger.info { "Send InnsendingFerdigstiltEvent til kafka for journalpostId $key" }
        sikkerLogger.info { message.toJson() }
    }

    private fun shutdownHook() {
        logger.info("received shutdown signal, stopping app")
        producer.close()
    }
}

private fun InnsendingObserver.InnsendingEvent.toPayload() =
    mutableMapOf<String, Any>(
        "@id" to UUID.randomUUID().toString(),
        "@opprettet" to LocalDateTime.now(),
        "journalpostId" to journalpostId,
        "datoRegistrert" to datoRegistrert,
        "skjemaKode" to skjemaKode,
        "tittel" to tittel,
        "type" to type.name
    ).apply {
        f??dselsnummer?.let { set("f??dselsnummer", it) }
        akt??rId?.let { set("akt??rId", it) }
        fagsakId?.let { set("fagsakId", it) }
        s??knadsData?.let { set("s??knadsData", it) }
    }
