package no.nav.dagpenger.mottak.serder

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import java.time.LocalDateTime
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

data class InnsendingData(
    val id: Long,
    val journalpostId: String,
    val tilstand: TilstandData,
    val journalpostData: JournalpostData?,
    val oppfyllerMinsteArbeidsinntekt: Boolean?,
    val eksisterendeSaker: Boolean?,
    val personData: PersonData?,
    val arenaSakData: ArenaSakData?,
    val søknadsData: JsonNode?,
    val aktivitetslogg: AktivitetsloggData
) {
    fun createInnsending(): Innsending {
        return Innsending::class.primaryConstructor!!
            .apply { isAccessible = true }
            .call(
                journalpostId,
                tilstand.createTilstand(),
                journalpostData?.let {
                    Journalpost(
                        journalpostId = journalpostId,
                        bruker = journalpostData.bruker?.createBruker(),
                        journalpostStatus = journalpostData.journalpostStatus,
                        behandlingstema = journalpostData.behandlingstema,
                        registrertDato = journalpostData.registertDato,
                        dokumenter = journalpostData.dokumenter.map {
                            Journalpost.DokumentInfo(
                                it.tittel,
                                it.dokumentInfoId,
                                it.brevkode
                            )
                        }
                    )
                },
                søknadsData?.let { Søknadsdata.Søknad(it) },
                oppfyllerMinsteArbeidsinntekt,
                eksisterendeSaker,
                personData?.let {
                    PersonInformasjon.Person(
                        it.aktørId,
                        it.fødselsnummer,
                        it.norskTilknytning,
                        it.diskresjonskode
                    )
                },
                arenaSakData?.let { ArenaOppgaveOpprettet.ArenaSak(it.oppgaveId, it.fagsakId) },
                aktivitetslogg.let(::konverterTilAktivitetslogg)
            )
    }

    data class TilstandData(
        val type: InnsendingTilstandTypeData
    ) {
        fun createTilstand(): Innsending.Tilstand = when (type) {
            InnsendingTilstandTypeData.MottattType -> Innsending.Mottatt
            InnsendingTilstandTypeData.AvventerJournalpostType -> Innsending.AvventerJournalpost
            InnsendingTilstandTypeData.AvventerPersondataType -> Innsending.AvventerPersondata
            InnsendingTilstandTypeData.KategoriseringType -> Innsending.Kategorisering
            InnsendingTilstandTypeData.AvventerSøknadsdataType -> Innsending.AvventerSøknadsdata
            InnsendingTilstandTypeData.AvventerMinsteinntektVurderingType -> Innsending.AventerMinsteinntektVurdering
            InnsendingTilstandTypeData.AvventerSvarOmEksisterendeSakerType -> Innsending.AvventerSvarOmEksisterendeSaker
            InnsendingTilstandTypeData.AventerArenaStartVedtakType -> Innsending.AventerArenaStartVedtak
            InnsendingTilstandTypeData.AvventerFerdigstillJournalpostType -> Innsending.AventerFerdigstill
            InnsendingTilstandTypeData.InnsendingFerdigstiltType -> Innsending.InnsendingFerdigStilt
            InnsendingTilstandTypeData.AventerArenaOppgaveType -> Innsending.AventerArenaOppgave
            InnsendingTilstandTypeData.AvventerGosysType -> Innsending.AvventerGosysOppgave
        }

        enum class InnsendingTilstandTypeData {
            MottattType,
            AvventerJournalpostType,
            AvventerPersondataType,
            KategoriseringType,
            AvventerSøknadsdataType,
            AvventerMinsteinntektVurderingType,
            AvventerSvarOmEksisterendeSakerType,
            AventerArenaStartVedtakType,
            AvventerFerdigstillJournalpostType,
            InnsendingFerdigstiltType,
            AventerArenaOppgaveType,
            AvventerGosysType
        }
    }

    data class AktivitetsloggData(
        val aktiviteter: List<AktivitetData>
    ) {
        data class AktivitetData(
            val alvorlighetsgrad: Alvorlighetsgrad,
            val label: Char,
            val behovtype: String?,
            val melding: String,
            val tidsstempel: String,
            val kontekster: List<SpesifikkKontekstData>,
            val detaljer: Map<String, Any>
        )

        data class SpesifikkKontekstData(
            val kontekstType: String,
            val kontekstMap: Map<String, String>
        )

        enum class Alvorlighetsgrad {
            INFO,
            WARN,
            BEHOV,
            ERROR,
            SEVERE
        }
    }

    data class ArenaSakData(
        val oppgaveId: String,
        val fagsakId: String
    )

    data class PersonData(
        val aktørId: String,
        val fødselsnummer: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: Boolean
    )

    data class JournalpostData(
        val journalpostId: String,
        val journalpostStatus: String,
        val bruker: BrukerData?,
        val behandlingstema: String?,
        val registertDato: LocalDateTime,
        val dokumenter: List<DokumentInfoData>
    ) {
        fun createJournalPost() {
            return
        }

        enum class BrukerTypeData {
            ORGNR, AKTOERID, FNR;

            fun createBrukerType() = when (this) {
                ORGNR -> Journalpost.BrukerType.ORGNR
                AKTOERID -> Journalpost.BrukerType.AKTOERID
                FNR -> Journalpost.BrukerType.FNR
            }
        }

        data class BrukerData(
            val type: BrukerTypeData,
            val id: String
        ) {
            fun createBruker() = Journalpost.Bruker(this.type.createBrukerType(), this.id)
        }

        data class DokumentInfoData(
            val tittel: String,
            val brevkode: String,
            val dokumentInfoId: String
        )
    }
}
