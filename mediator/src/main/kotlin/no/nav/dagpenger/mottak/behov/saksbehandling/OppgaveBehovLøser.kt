package no.nav.dagpenger.mottak.behov.saksbehandling

import com.github.navikt.tbd_libs.rapids_and_rivers.JsonMessage
import com.github.navikt.tbd_libs.rapids_and_rivers.River
import com.github.navikt.tbd_libs.rapids_and_rivers.asLocalDateTime
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageContext
import com.github.navikt.tbd_libs.rapids_and_rivers_api.MessageMetadata
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.micrometer.core.instrument.MeterRegistry
import kotlinx.coroutines.runBlocking
import mu.KotlinLogging
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaOppslag
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.arenaOppgaveParametre
import no.nav.dagpenger.mottak.meldinger.OppgaveOpprettet
import java.time.LocalDateTime
import java.util.UUID

private val logger = KotlinLogging.logger { }

internal interface OppgaveKlient {
    suspend fun opprettOppgave(
        sakId: UUID,
        journalpostId: String,
        opprettetTidspunkt: LocalDateTime,
        ident: String,
        skjemaKategori: String,
    ): UUID

}

internal class OppgaveBehovLøser(
    private val arenaOppslag: ArenaOppslag,
    private val oppgaveKlient: OppgaveKlient,
    rapidsConnection: RapidsConnection,
) : River.PacketListener {
    init {
        River(rapidsConnection)
            .apply {
                precondition { it.requireValue("@event_name", "behov") }
                precondition {
                    it.requireAllOrAny(
                        "@behov",
                        listOf(Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettOppgave.name),
                    )
                }
                precondition { it.forbid("@løsning") }
                precondition { it.forbid("@feil") }
                validate { it.requireKey("@behovId", "journalpostId") }
                validate {
                    it.requireKey(
                        "fødselsnummer",
                        "behandlendeEnhetId",
                        "oppgavebeskrivelse",
                        "registrertDato",
                        "tilleggsinformasjon",
                        "skjemaKategori",
                    )
                }
            }.register(this)
    }


    override fun onPacket(
        packet: JsonMessage,
        context: MessageContext,
        metadata: MessageMetadata,
        meterRegistry: MeterRegistry,
    ) {
        val journalpostId = packet["journalpostId"].asText()
        val behovId = packet["@behovId"].asText()
        val registrertTidspunkt = packet["registrertDato"].asLocalDateTime()

        val env = "dev"
        // Vi får sak fra et eller annet sted
        val sakId = UUID.randomUUID()
        if (env == "dev") {
            val oppgaveId = this.oppgaveKlient.opprettOppgave(
                sakId = sakId,
                journalpostId = journalpostId,
                opprettetTidspunkt = registrertTidspunkt,
                ident = packet["fødselsnummer"].asText(),
                skjemaKategori = packet["skjemaKategori"].asText(),
            )
            OppgaveOpprettet.Sak(
                oppgaveId = oppgaveId,
                sakId = sakId,
            )

        } else {
            val behovNavn = "OpprettVurderhenvendelseOppgave"
            runBlocking {
                val oppgaveResponse = arenaOppslag.opprettVurderHenvendelsOppgave(
                    journalpostId,
                    packet.arenaOppgaveParametre(),
                )
                if (oppgaveResponse != null) {
                    packet["@løsning"] =
                        mapOf(
                            behovNavn to
                                    mapOf(
                                        "journalpostId" to journalpostId,
                                        "fagsakId" to oppgaveResponse.fagsakId,
                                        "oppgaveId" to oppgaveResponse.oppgaveId,
                                    ),
                        ).also {
                            logger.info { "Løste behov $behovNavn med løsning $it" }
                        }
                } else {
                    packet["@løsning"] =
                        mapOf(
                            behovNavn to mapOf("@feil" to "Kunne ikke opprettet Arena oppgave"),
                        ).also {
                            logger.info { "Løste behov $behovNavn med feil $it" }
                        }
                }
            }
        }
    }
}