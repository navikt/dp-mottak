package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.github.oshai.kotlinlogging.withLoggingContext
import io.ktor.client.plugins.ClientRequestException
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostDokarkiv
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import no.nav.dagpenger.mottak.serder.asUUID
import no.nav.dagpenger.mottak.tjenester.VedtakFattetEvent.Companion.tilVedtakFattetEvent

private val logger = KotlinLogging.logger {}


enum class VedtakFattetEvent(val eventName: String) {
    VEDTAK_FATTET_UTENFOR_ARENA("vedtak_fattet_utenfor_arena"),
    VEDTAK_FATTET_I_NY_SAK("vedtak_fattet_i_ny_sak");

    companion object {
        fun String.tilVedtakFattetEvent(): VedtakFattetEvent {
            return entries.single { it.eventName == this }
        }
    }
}

internal class VedtakFattetMottak(
    rapidsConnection: RapidsConnection,
    private val innsendingMetadataRepository: InnsendingMetadataRepository,
    private val journalpostDokarkiv: JournalpostDokarkiv,
) : River.PacketListener {

    companion object {

        val rapidFilter: River.() -> Unit = {
            precondition {
                it.requireAny(
                    "@event_name",
                    listOf(
                        VedtakFattetEvent.VEDTAK_FATTET_UTENFOR_ARENA.eventName,
                        VedtakFattetEvent.VEDTAK_FATTET_I_NY_SAK.eventName,
                    )
                )
            }
            validate {
                it.requireKey("behandlingId", "søknadId", "ident", "sakId")
            }
        }
    }

    init {
        River(rapidsConnection).apply(rapidFilter).register(this)
    }


    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val søknadId = packet["søknadId"].asUUID()
        val behandlingId = packet["behandlingId"].asUUID()
        val ident = packet["ident"].asString()
        val dagpengerFagsakId = packet["sakId"].asUUID()
        withLoggingContext("søknadId" to "$søknadId", "behandlingId" to "$behandlingId") {
            val vedtakFattetEvent = packet["@event_name"].stringValue().tilVedtakFattetEvent()
            logger.info { "Mottok $vedtakFattetEvent" }

            when (vedtakFattetEvent) {
                VedtakFattetEvent.VEDTAK_FATTET_UTENFOR_ARENA -> {
                    val arenaOppgaver =
                        innsendingMetadataRepository.hentArenaOppgaver(
                            søknadId = søknadId,
                            ident = ident,
                        )

                    if (arenaOppgaver.isNotEmpty()) {
                        val oppgaveIder = arenaOppgaver.map { it.oppgaveId }
                        runBlocking {
                            arenaOppgaver.forEach { oppgave ->
                                runCatching {
                                    val knyttJounalPostTilNySakResponse =
                                        journalpostDokarkiv.knyttJounalPostTilNySak(
                                            journalpostId = oppgave.journalpostId,
                                            dagpengerFagsakId = dagpengerFagsakId.toString(),
                                            ident = ident,
                                        )
                                    innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(
                                        nyJournalpostId = knyttJounalPostTilNySakResponse.nyJournalpostId,
                                        innsendingId = oppgave.innsendingId,
                                        sakId = dagpengerFagsakId,
                                    )
                                }.getOrElse(Throwable::ignorerManglendeJournalPostIDev)
                            }
                        }

                        val message =
                            JsonMessage
                                .newNeed(
                                    behov = listOf("slett_arena_oppgaver"),
                                    map =
                                        mapOf(
                                            "behandlingId" to behandlingId,
                                            "oppgaveIder" to oppgaveIder,
                                            "ident" to ident,
                                        ),
                                ).toJson()
                        context.publish(ident, message)
                    }
                }
                VedtakFattetEvent.VEDTAK_FATTET_I_NY_SAK -> {
                    runBlocking {
                        innsendingMetadataRepository.finnJournalpostIderForSøknad(ident = ident, søknadId = søknadId)
                    }
                }
            }
        }
    }
}

private fun Throwable.ignorerManglendeJournalPostIDev() {
    when {
        Config.isDev &&
                this is ClientRequestException &&
                response.status.value == 404 -> {
            logger.warn(this) { "Ignorerer 404 fra journalpostDokarkiv i dev" }
        }

        else -> throw this
    }
}
