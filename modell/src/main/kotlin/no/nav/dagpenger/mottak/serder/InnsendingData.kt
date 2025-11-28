package no.nav.dagpenger.mottak.serder

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.DagpengerOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.søknadsdata.rutingOppslagV2
import java.time.LocalDateTime
import java.util.UUID
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

data class InnsendingData(
    val id: Long,
    val journalpostId: String,
    val tilstand: TilstandData,
    val journalpostData: JournalpostData?,
    val personData: PersonData?,
    val arenaSakData: ArenaSakData?,
    val søknadsData: JsonNode?,
    val mottakskanal: String?,
    val oppgaveSakData: OppgaveSakData? = null,
    val aktivitetslogg: AktivitetsloggData,
) {
    fun createInnsending(): Innsending =
        Innsending::class
            .primaryConstructor!!
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
                        journalførendeEnhet = journalpostData.journalførendeEnhet,
                        dokumenter =
                            journalpostData.dokumenter.map {
                                Journalpost.DokumentInfo(
                                    it.tittel,
                                    it.dokumentInfoId,
                                    it.brevkode,
                                    it.hovedDokument,
                                )
                            },
                    )
                },
                søknadsData?.let { rutingOppslagV2(it) },
                personData?.let {
                    PersonInformasjon.Person(
                        it.navn,
                        it.aktørId,
                        it.fødselsnummer,
                        it.norskTilknytning,
                        it.diskresjonskode,
                        it.egenAnsatt,
                    )
                },
                arenaSakData?.let { ArenaOppgaveOpprettet.ArenaSak(oppgaveId = it.oppgaveId, fagsakId = it.fagsakId) },
                mottakskanal,
                oppgaveSakData?.let { DagpengerOppgaveOpprettet.OppgaveSak(oppgaveId = it.oppgaveId, fagsakId = it.fagsakId) },
                aktivitetslogg.let(::konverterTilAktivitetslogg),
            )

    data class TilstandData(
        val type: InnsendingTilstandTypeData,
    ) {
        fun createTilstand(): Innsending.Tilstand =
            when (type) {
                InnsendingTilstandTypeData.AlleredeBehandletType -> Innsending.AlleredeBehandlet
                InnsendingTilstandTypeData.AventerArenaOppgaveType -> Innsending.AventerVurderHenvendelseArenaOppgave
                InnsendingTilstandTypeData.AventerArenaStartVedtakType -> Innsending.AventerArenaStartVedtak
                InnsendingTilstandTypeData.AvventerFerdigstillJournalpostType -> Innsending.AventerFerdigstill
                InnsendingTilstandTypeData.AvventerGosysType -> Innsending.AvventerGosysOppgave
                InnsendingTilstandTypeData.AvventerJournalpostType -> Innsending.AvventerJournalpost
                InnsendingTilstandTypeData.AvventerMinsteinntektVurderingType -> Innsending.AventerArenaStartVedtak
                InnsendingTilstandTypeData.AvventerPersondataType -> Innsending.AvventerPersondata
                InnsendingTilstandTypeData.AvventerSvarOmEksisterendeSakerType -> Innsending.AventerArenaStartVedtak
                InnsendingTilstandTypeData.AvventerSøknadsdataType -> Innsending.AvventerSøknadsdata
                InnsendingTilstandTypeData.InnsendingFerdigstiltType -> Innsending.InnsendingFerdigStilt
                InnsendingTilstandTypeData.KategoriseringType -> Innsending.Kategorisering
                InnsendingTilstandTypeData.MottattType -> Innsending.Mottatt
                InnsendingTilstandTypeData.UkjentBrukerType -> Innsending.UkjentBruker
                InnsendingTilstandTypeData.HåndterInnsendingType -> Innsending.HåndterInnsending
            }

        enum class InnsendingTilstandTypeData {
            AlleredeBehandletType,
            AventerArenaOppgaveType,
            AventerArenaStartVedtakType,
            AvventerFerdigstillJournalpostType,
            AvventerGosysType,
            AvventerJournalpostType,
            AvventerMinsteinntektVurderingType,
            AvventerPersondataType,
            AvventerSvarOmEksisterendeSakerType,
            AvventerSøknadsdataType,
            InnsendingFerdigstiltType,
            KategoriseringType,
            MottattType,
            UkjentBrukerType,
            HåndterInnsendingType,
        }
    }

    data class AktivitetsloggData(
        val aktiviteter: List<AktivitetData>,
    ) {
        data class AktivitetData(
            val alvorlighetsgrad: Alvorlighetsgrad,
            val label: Char,
            val behovtype: String?,
            val melding: String,
            val tidsstempel: String,
            val kontekster: List<SpesifikkKontekstData>,
            val detaljer: Map<String, Any>,
        )

        data class SpesifikkKontekstData(
            val kontekstType: String,
            val kontekstMap: Map<String, String>,
        )

        enum class Alvorlighetsgrad {
            INFO,
            WARN,
            BEHOV,
            ERROR,
            SEVERE,
        }
    }

    data class ArenaSakData(
        val oppgaveId: String,
        val fagsakId: String?,
    )

    data class OppgaveSakData(
        val oppgaveId: UUID?,
        val fagsakId: UUID,
    )

    data class PersonData(
        val navn: String,
        val aktørId: String,
        val fødselsnummer: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: Boolean,
        val egenAnsatt: Boolean,
    )

    data class JournalpostData(
        val journalpostId: String,
        val journalpostStatus: String,
        val bruker: BrukerData?,
        val behandlingstema: String?,
        val registertDato: LocalDateTime,
        val journalførendeEnhet: String?,
        val dokumenter: List<DokumentInfoData>,
    ) {
        enum class BrukerTypeData {
            ORGNR,
            AKTOERID,
            FNR,
            ;

            fun createBrukerType() =
                when (this) {
                    ORGNR -> Journalpost.BrukerType.ORGNR
                    AKTOERID -> Journalpost.BrukerType.AKTOERID
                    FNR -> Journalpost.BrukerType.FNR
                }
        }

        data class BrukerData(
            val type: BrukerTypeData,
            val id: String,
        ) {
            fun createBruker() = Journalpost.Bruker(this.type.createBrukerType(), this.id)
        }

        data class DokumentInfoData(
            val tittel: String,
            val brevkode: String,
            val dokumentInfoId: String,
            val hovedDokument: Boolean,
        )
    }
}
