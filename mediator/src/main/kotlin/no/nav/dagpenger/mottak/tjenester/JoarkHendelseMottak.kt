package no.nav.dagpenger.mottak.tjenester

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import org.apache.avro.generic.GenericRecord
import org.apache.kafka.clients.consumer.Consumer
import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.consumer.ConsumerRecords
import org.apache.kafka.clients.consumer.KafkaConsumer
import org.apache.kafka.common.TopicPartition
import org.apache.kafka.common.errors.WakeupException
import java.time.Duration
import java.util.Properties
import kotlin.coroutines.CoroutineContext

internal class JoarkHendelseMottak internal constructor(
    private val innsendingMediator: InnsendingMediator,
    private val consumer: Consumer<String, GenericRecord>
) : CoroutineScope {

    override val coroutineContext: CoroutineContext
        get() = Dispatchers.IO + job
    private val job: Job = Job()

    init {
        Runtime.getRuntime().addShutdownHook(Thread(::shutdownHook))
    }

    constructor(
        topicName: String,
        kafkaProperties: Properties,
        innsendingMediator: InnsendingMediator,
    ) : this(innsendingMediator, createConsumer(topicName, kafkaProperties))

    private companion object {

        private const val maxPollRecords = 50
        private val maxPollIntervalMs = Duration.ofSeconds(60 + maxPollRecords * 2.toLong()).toMillis()
        private val logger = KotlinLogging.logger {}
        private fun createConsumer(topicName: String, properties: Properties): KafkaConsumer<String, GenericRecord> {
            properties[ConsumerConfig.GROUP_ID_CONFIG] = "dp-mottak-joark-v1"
            properties[ConsumerConfig.AUTO_OFFSET_RESET_CONFIG] = "earliest"
            properties[ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG] = "false"
            properties[ConsumerConfig.MAX_POLL_RECORDS_CONFIG] = maxPollRecords
            properties[ConsumerConfig.MAX_POLL_INTERVAL_MS_CONFIG] = "$maxPollIntervalMs"
            return KafkaConsumer<String, GenericRecord>(properties).also {
                it.subscribe(listOf(topicName))
            }
        }
    }

    private fun run() {
        try {
            while (job.isActive) {
                onRecords(consumer.poll(Duration.ofSeconds(1)))
            }
        } catch (e: WakeupException) {
            if (job.isActive) throw e
        } catch (e: Exception) {
            logger.error(e) { "Noe feil skjedde i consumeringen" }
            throw e
        } finally {
            closeResources()
        }
    }


    fun isAlive() = job.isActive

    private fun onRecords(records: ConsumerRecords<String, GenericRecord>) {
        if (records.isEmpty) return // poll returns an empty collection in case of rebalancing
        logger.info { "Fikk ${records.count()} meldinger" }
        val currentPositions = records
            .groupBy { TopicPartition(it.topic(), it.partition()) }
            .mapValues { partition -> partition.value.minOf { it.offset() } }
            .toMutableMap()
        try {
            records.onEach { record ->
                if (record.value().kanBehandle()) {
                    with(record.value()) {
                        logger.info(
                            """Received journalpost with journalpost id: ${this.journalPostId()} 
                              |tema: ${this["temaNytt"]}, 
                              |hendelsesType: ${this["hendelsesType"]}, 
                              |mottakskanal, ${this["mottaksKanal"]}, 
                              |behandlingstema: ${this.behandlingstema()}
                              |""".trimMargin()
                        )
                        val joarkHendelse = JoarkHendelse(
                            aktivitetslogg = Aktivitetslogg(),
                            journalpostId = this.journalPostId(),
                            hendelseType = this.hendelseType(),
                            journalpostStatus = this.journalpostStatus(),
                            behandlingstema = this.behandlingstema(),
                        )
                        innsendingMediator.håndter(
                            joarkHendelse
                        )
                    }
                }
                currentPositions[TopicPartition(record.topic(), record.partition())] = record.offset() + 1
            }
        } catch (err: Exception) {
            logger.info(
                "due to an error during processing, positions are reset to each next message (after each record that was processed OK):" +
                    currentPositions.map { "\tpartition=${it.key}, offset=${it.value}" }
                        .joinToString(separator = "\n", prefix = "\n", postfix = "\n"),
                err
            )
            currentPositions.forEach { (partition, offset) -> consumer.seek(partition, offset) }
            throw err
        } finally {
            consumer.commitSync()
        }
    }

    private val forbudteMottaksKanaler = setOf(
        "EESSI",
        "NAV_NO_CHAT"
    )
    private fun GenericRecord.erIkkeForbudtKanal(): Boolean = this.get("mottaksKanal").toString() !in forbudteMottaksKanaler
    private fun GenericRecord.erTemaDagpenger(): Boolean = "DAG" == this.get("temaNytt").toString()
    private fun GenericRecord.harStatusJournalpostMottatt(): Boolean = "JournalpostMottatt" == this.get("hendelsesType").toString()
    private fun GenericRecord.kanBehandle(): Boolean = this.erTemaDagpenger() && this.harStatusJournalpostMottatt() && this.erIkkeForbudtKanal()
    private fun GenericRecord.journalPostId() = this.get("journalpostId").toString()
    private fun GenericRecord.hendelseType() = this.get("hendelsesType").toString()
    private fun GenericRecord.journalpostStatus() = this.get("journalpostStatus").toString()
    private fun GenericRecord.behandlingstema() = get("behandlingstema")?.toString()

    private fun closeResources() {
        tryAndLog(consumer::unsubscribe)
        tryAndLog(consumer::close)
    }

    private fun tryAndLog(block: () -> Unit) {
        try {
            block()
        } catch (err: Exception) {
            logger.error(err.message, err)
        }
    }

    private fun shutdownHook() {
        logger.info("received shutdown signal, stopping app")
        stop()
    }

    fun stop() {
        logger.info("stopping JournalfoeringReplicator")
        consumer.wakeup()
        job.cancel()
    }

    fun start() {
        logger.info("starting JournalfoeringReplicator")
        launch {
            run()
        }
    }
}
