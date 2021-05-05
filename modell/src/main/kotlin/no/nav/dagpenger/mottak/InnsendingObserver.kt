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

    data class InnsendingFerdigstiltEvent(
        val type: Type,
        val journalpostId: String,
        val aktørId: String?,
        val fødselsnummer: String?,
        val fagsakId: String?,
        val datoRegistrert: LocalDateTime,
        val søknadsData: JsonNode?
    )

    data class InnsendingMottattEvent(
        val type: Type,
        val journalpostId: String,
        val aktørId: String?,
        val fødselsnummer: String?,
        val datoRegistrert: LocalDateTime,
        val søknadsData: JsonNode?
    )

    fun tilstandEndret(event: InnsendingEndretTilstandEvent) {}
    fun innsendingFerdigstilt(event: InnsendingFerdigstiltEvent) {}
    fun innsendingMottatt(event: InnsendingMottattEvent) {}
}
