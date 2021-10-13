package no.nav.dagpenger.mottak.tjenester

import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import org.apache.avro.Schema
import org.apache.avro.generic.GenericData
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.ConsumerRecord
import org.apache.kafka.clients.consumer.MockConsumer
import org.apache.kafka.clients.consumer.OffsetResetStrategy
import org.apache.kafka.common.TopicPartition
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import kotlin.random.Random

internal class JoarkHendelseMottakTest {
    private val journalfoeringTopic = "joark-topic"
    private val journalfoeringPartition = TopicPartition(journalfoeringTopic, 0)
    private val mockConsumer = MockConsumer<String, GenericRecord>(OffsetResetStrategy.EARLIEST).also {
        it.assign(listOf(journalfoeringPartition))
        it.updateBeginningOffsets(
            mapOf(
                journalfoeringPartition to 0L,
            )
        )
    }

    @Test
    fun `skal skippe meldinger fra joark med andre temaer enn DAG`() = runBlocking {
        val innsendingMediator = mockk<InnsendingMediator>(relaxed = true)
        JoarkHendelseMottak(innsendingMediator, mockConsumer).also { it.start() }

        listOf("DAG", "IKKEDAG").forEachIndexed { offset, tema ->
            mockConsumer.addRecord(
                ConsumerRecord(
                    journalfoeringTopic, 0, offset.toLong(), "jpid",
                    lagJoarkHendelse(
                        journalpostId = Random.nextLong(),
                        tema = tema,
                        hendelsesType = "JournalpostMottatt"
                    )
                )
            )
        }
        delay(1000)
        val offsetData = mockConsumer.committed(setOf(journalfoeringPartition))
        assertEquals(2L, offsetData[journalfoeringPartition]?.offset())
        verify(exactly = 1) { innsendingMediator.håndter(any() as JoarkHendelse) }
    }

    @Test
    fun `skal skippe meldinger fra joark med andre hendelsetyper enn 'JournalpostMottatt'`() = runBlocking {
        val innsendingMediator = mockk<InnsendingMediator>(relaxed = true)
        JoarkHendelseMottak(innsendingMediator, mockConsumer).also { it.start() }

        mockConsumer.addRecord(
            ConsumerRecord(
                journalfoeringTopic, 0, 0L, "jpid",
                lagJoarkHendelse(
                    journalpostId = Random.nextLong(),
                    tema = "DAG",
                    hendelsesType = "JournalpostMottatt"
                )
            )
        )

        mockConsumer.addRecord(
            ConsumerRecord(
                journalfoeringTopic, 0, 1L, "jpid",
                lagJoarkHendelse(
                    journalpostId = Random.nextLong(),
                    tema = "DAG",
                    hendelsesType = "EndeligJournalført"
                )
            )
        )

        delay(1000)
        val offsetData = mockConsumer.committed(setOf(journalfoeringPartition))
        assertEquals(2L, offsetData[journalfoeringPartition]?.offset())
        verify(exactly = 1) { innsendingMediator.håndter(any() as JoarkHendelse) }
    }

    @ParameterizedTest
    @ValueSource(strings = ["EESSI", "NAV_NO_CHAT"])
    fun `skal skippe meldinger fra joark med mottakstype`(kanal: String) = runBlocking {
        val innsendingMediator = mockk<InnsendingMediator>(relaxed = true)
        JoarkHendelseMottak(innsendingMediator, mockConsumer).also { it.start() }

        mockConsumer.addRecord(
            ConsumerRecord(
                journalfoeringTopic, 0, 0L, "jpid",
                lagJoarkHendelse(
                    journalpostId = Random.nextLong(),
                    tema = "DAG",
                    hendelsesType = "JournalpostMottatt",
                    mottakskanal = kanal
                )
            )
        )

        delay(500)

        verify(exactly = 0) { innsendingMediator.håndter(any() as JoarkHendelse) }
    }

    @BeforeEach
    fun reset() {
        mockConsumer.updateBeginningOffsets(
            mapOf(
                journalfoeringPartition to 0L,
            )
        )
    }
}

private fun lagJoarkHendelse(
    journalpostId: Long,
    tema: String,
    hendelsesType: String,
    mottakskanal: String = "NAV_NO"
): GenericData.Record {
    return GenericData.Record(joarkjournalfoeringhendelserAvroSchema).apply {
        put("journalpostId", journalpostId)
        put("hendelsesId", journalpostId.toString())
        put("versjon", journalpostId.toInt())
        put("hendelsesType", hendelsesType)
        put("journalpostStatus", "journalpostStatus")
        put("temaGammelt", tema)
        put("temaNytt", tema)
        put("mottaksKanal", mottakskanal)
        put("kanalReferanseId", "kanalReferanseId")
        put("behandlingstema", tema)
    }
}

// From https://stash.adeo.no/projects/BOAF/repos/dok-avro/browse/dok-journalfoering-hendelse-v1/src/main/avro/schema/v1/JournalfoeringHendelse.avsc
private val joarkjournalfoeringhendelserSchema =
    // language=json
    """

            {
              "namespace" : "no.nav.joarkjournalfoeringhendelser",
              "type" : "record",
              "name" : "JournalfoeringHendelseRecord",
              "fields" : [
                {"name": "hendelsesId", "type": "string"},
                {"name": "versjon", "type": "int"},
                {"name": "hendelsesType", "type": "string"},
                {"name": "journalpostId", "type": "long"},
                {"name": "journalpostStatus", "type": "string"},
                {"name": "temaGammelt", "type": "string"},
                {"name": "temaNytt", "type": "string"},
                {"name": "mottaksKanal", "type": "string"},
                {"name": "kanalReferanseId", "type": "string"},
                {"name": "behandlingstema", "type": "string", "default": ""}
              ]
            }

    """.trimIndent()

private val joarkjournalfoeringhendelserAvroSchema = Schema.Parser().parse(joarkjournalfoeringhendelserSchema)
