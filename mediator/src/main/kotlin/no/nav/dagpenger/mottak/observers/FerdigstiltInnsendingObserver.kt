package no.nav.dagpenger.mottak.observers

import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.helse.rapids_rivers.JsonMessage
import org.apache.kafka.clients.producer.Producer
import org.apache.kafka.clients.producer.ProducerRecord
import java.time.LocalDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit

class FerdigstiltInnsendingObserver internal constructor(private val producer: Producer<String, String>) :
    InnsendingObserver {

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
}
