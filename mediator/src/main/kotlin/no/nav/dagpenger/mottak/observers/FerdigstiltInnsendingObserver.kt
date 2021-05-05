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
        private fun createProducer(producerProperties: Properties): KafkaProducer<String, String> {
            producerProperties[ProducerConfig.ACKS_CONFIG] = "all"
            producerProperties[ProducerConfig.LINGER_MS_CONFIG] = "0"
            producerProperties[ProducerConfig.MAX_IN_FLIGHT_REQUESTS_PER_CONNECTION] = "1"
            return KafkaProducer(producerProperties, StringSerializer(), StringSerializer())
        }
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingFerdigstiltEvent) {

        val message = JsonMessage.newMessage(
            event.toParameterMap()
        )

        producer.send(
            ProducerRecord(
                "teamdagpenger.journalforing.v1",
                event.journalpostId,
                message.toJson()
            )
        ).get(500, TimeUnit.MILLISECONDS)
    }

    private fun shutdownHook() {
        logger.info("received shutdown signal, stopping app")
        producer.close()
    }
}

private fun InnsendingObserver.InnsendingFerdigstiltEvent.toParameterMap(): Map<String, Any> {

    val parametere = mutableMapOf<String, Any>()

    parametere["@event_name"] = "innsending_ferdigstilt"
    parametere["@id"] = UUID.randomUUID().toString()
    parametere["@opprettet"] = LocalDateTime.now()
    parametere["journalpostId"] = journalpostId
    parametere["datoRegistrert"] = datoRegistrert
    parametere["type"] = type.name
    fødselsnummer?.let { parametere["fødselsnummer"] = it }
    aktørId?.let { parametere["aktørId"] = it }
    fagsakId?.let { parametere["fagsakId"] = it }
    søknadsData?.let { parametere["søknadsData"] = it }

    return parametere.toMap()
}
