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

private val sikkerlogg = KotlinLogging.logger("tjenestekall.FerdigstiltInnsendingObserver")

internal class FerdigstiltInnsendingObserver internal constructor(private val producer: Producer<String, String>) :
    InnsendingObserver {
        constructor(producerProperties: Properties) : this(createProducer(producerProperties))

        init {
            Runtime.getRuntime().addShutdownHook(Thread(::shutdownHook))
        }

        private companion object {
            private val logger = KotlinLogging.logger {}

            private fun createProducer(producerProperties: Properties): KafkaProducer<String, String> {
                producerProperties[ProducerConfig.ACKS_CONFIG] = "all"
                producerProperties[ProducerConfig.LINGER_MS_CONFIG] = "0"
                producerProperties[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "1"
                return KafkaProducer(producerProperties, StringSerializer(), StringSerializer())
            }
        }

        override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
            val payload =
                event.toPayload().also {
                    it["@event_name"] = "innsending_ferdigstilt"
                    it["bruk-dp-behandling"] = true
                }

            publish(
                key = event.journalpostId,
                message =
                    JsonMessage.newMessage(
                        payload,
                    ),
            )
        }

        override fun innsendingMottatt(event: InnsendingObserver.InnsendingEvent) {
            val payload =
                event.toPayload().also {
                    it["@event_name"] = "innsending_mottatt"
                }

            publish(
                key = event.journalpostId,
                message =
                    JsonMessage.newMessage(
                        payload,
                    ),
            )
        }

        private fun publish(
            key: String,
            message: JsonMessage,
        ) {
            message.requireKey("@event_name")
            producer.send(
                ProducerRecord(
                    "teamdagpenger.journalforing.v1",
                    key,
                    message.toJson(),
                ),
            ).get(500, TimeUnit.MILLISECONDS)

            logger.info { "Send ${message["@event_name"].asText()} til Kafka for journalpostId=$key" }
            sikkerlogg.info { "Sent ${message.toJson()}} til Kafka " }
        }

        private fun shutdownHook() {
            logger.info("received shutdown signal, stopping app")
            producer.flush()
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
        "type" to type.name,
    ).apply {
        fødselsnummer?.let { set("fødselsnummer", it) }
        aktørId?.let { set("aktørId", it) }
        fagsakId?.let { set("fagsakId", it) }
        søknadsData?.let { set("søknadsData", it) }
    }
