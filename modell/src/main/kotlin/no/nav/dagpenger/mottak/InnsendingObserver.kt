package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode
import java.time.Duration
import java.time.LocalDateTime

interface InnsendingObserver {

    data class InnsendingEndretTilstandEvent(
        val journalpostId: String,
        val gjeldendeTilstand: InnsendingTilstandType,
        val forrigeTilstand: InnsendingTilstandType,
        val aktivitetslogg: Aktivitetslogg,
        val timeout: Duration
    )

    data class InnsendingFerdigstiltEvent(
        val type: Type,
        val journalpostId: String,
        val aktørId: String?,
        val fødselsnummer: String?,
        val fagsakId: String?,
        val datoRegistrert: LocalDateTime,
        val søknadsData: JsonNode?,
    ) {

        enum class Type {
            NySøknad,
            Gjenopptak,
            Utdanning,
            Etablering,
            KlageOgAnke,
            KlageOgAnkeLønnskompensasjon,
            Ettersending,
            UkjentSkjemaKode,
            UtenBruker
        }
    }

    fun tilstandEndret(event: InnsendingEndretTilstandEvent) {}
    fun innsendingFerdigstilt(event: InnsendingFerdigstiltEvent) {}
}
