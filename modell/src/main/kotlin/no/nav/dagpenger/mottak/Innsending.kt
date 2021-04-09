package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.EksisterendeSaker
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Ferdigstill
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Gosysoppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.MinsteinntektVurdering
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettStartVedtakOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettVurderhenvendelseOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Søknadsdata
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet.ArenaSak
import no.nav.dagpenger.mottak.meldinger.EksisterendesakData
import no.nav.dagpenger.mottak.meldinger.GosysOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.MinsteinntektVurderingData
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon.Person
import java.time.Duration

class Innsending private constructor(
    private val journalpostId: String,
    private var tilstand: Tilstand,
    private var journalpostData: JournalpostData?,
    private var søknad: no.nav.dagpenger.mottak.meldinger.Søknadsdata.Søknad?,
    private var oppfyllerMinsteArbeidsinntekt: Boolean?,
    private var eksisterendeSaker: Boolean?,
    private var person: Person?,
    private var arenaSak: ArenaSak?,
    private var oppdatertJournalpost: Boolean?,
    private var ferdigstilt: Boolean?,
    internal val aktivitetslogg: Aktivitetslogg
) : Aktivitetskontekst {

    private val observers = mutableSetOf<InnsendingObserver>()

    constructor(
        journalpostId: String
    ) : this(
        journalpostId = journalpostId,
        tilstand = Mottatt,
        journalpostData = null,
        søknad = null,
        oppfyllerMinsteArbeidsinntekt = null,
        eksisterendeSaker = null,
        person = null,
        arenaSak = null,
        oppdatertJournalpost = null,
        ferdigstilt = null,
        aktivitetslogg = Aktivitetslogg()
    )

    fun journalpostId(): String = journalpostId

    fun håndter(joarkHendelse: JoarkHendelse) {
        if (journalpostId != joarkHendelse.journalpostId()) return
        kontekst(joarkHendelse, "Registrert joark hendelse")
        tilstand.håndter(this, joarkHendelse)
    }

    fun håndter(journalpostData: JournalpostData) {
        if (journalpostId != journalpostData.journalpostId()) return
        kontekst(journalpostData, "Mottatt informasjon om journalpost")
        tilstand.håndter(this, journalpostData)
    }

    fun håndter(personInformasjon: PersonInformasjon) {
        if (journalpostId != personInformasjon.journalpostId()) return
        kontekst(personInformasjon, "Mottatt informasjon om person")
        tilstand.håndter(this, personInformasjon)
    }

    fun håndter(søknadsdata: no.nav.dagpenger.mottak.meldinger.Søknadsdata) {
        if (journalpostId != søknadsdata.journalpostId()) return
        kontekst(søknadsdata, "Mottatt søknadsdata")
        tilstand.håndter(this, søknadsdata)
    }

    fun håndter(vurderminsteinntektData: MinsteinntektVurderingData) {
        if (journalpostId != vurderminsteinntektData.journalpostId()) return
        kontekst(vurderminsteinntektData, "Mottatt informasjon vurdering om minste arbeidsinntekt")
        tilstand.håndter(this, vurderminsteinntektData)
    }

    fun håndter(eksisterendeSak: EksisterendesakData) {
        if (journalpostId != eksisterendeSak.journalpostId()) return
        kontekst(eksisterendeSak, "Mottatt informasjon om eksisterende saker")
        tilstand.håndter(this, eksisterendeSak)
    }

    fun håndter(arenaOppgave: ArenaOppgaveOpprettet) {
        if (journalpostId != arenaOppgave.journalpostId()) return
        kontekst(arenaOppgave, "Mottatt informasjon om opprettet Arena oppgave")
        tilstand.håndter(this, arenaOppgave)
    }

    fun håndter(oppdatertJournalpost: JournalpostOppdatert) {
        if (journalpostId != oppdatertJournalpost.journalpostId()) return
        kontekst(oppdatertJournalpost, "Mottatt informasjon om oppdatert journalpost")
        tilstand.håndter(this, oppdatertJournalpost)
    }

    fun håndter(journalpostferdigstilt: no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt) {
        if (journalpostId != journalpostferdigstilt.journalpostId()) return
        kontekst(journalpostferdigstilt, "Mottatt informasjon om ferdigstilt journalpost")
        tilstand.håndter(this, journalpostferdigstilt)
    }

    fun håndter(gosysoppgaveopprettet: GosysOppgaveOpprettet) {
        if (journalpostId != gosysoppgaveopprettet.journalpostId()) return
        kontekst(gosysoppgaveopprettet, "Mottatt informasjon om opprettet Gosys oppgave")
        tilstand.håndter(this, gosysoppgaveopprettet)
    }

    private fun kontekst(hendelse: Hendelse, melding: String) {
        hendelse.kontekst(this)
        hendelse.kontekst(this.tilstand)
        hendelse.info(melding)
    }

    interface Tilstand : Aktivitetskontekst {

        val type: InnsendingTilstandType
        val timeout: Duration

        fun håndter(innsending: Innsending, joarkHendelse: JoarkHendelse) {
            joarkHendelse.warn("Forventet ikke JoarkHendelse i %s", type.name)
        }

        fun håndter(innsending: Innsending, journalpostData: JournalpostData) {
            journalpostData.warn("Forventet ikke JournalpostData i %s", type.name)
        }

        fun håndter(innsending: Innsending, personInformasjon: PersonInformasjon) {
            personInformasjon.warn("Forventet ikke PersonInformasjon i %s", type.name)
        }

        fun håndter(innsending: Innsending, søknadsdata: no.nav.dagpenger.mottak.meldinger.Søknadsdata) {
            søknadsdata.warn("Forventet ikke Søknadsdata i %s", type.name)
        }

        fun håndter(innsending: Innsending, vurderminsteinntektData: MinsteinntektVurderingData) {
            vurderminsteinntektData.warn("Forventet ikke MinsteinntektVurderingData i %s", type.name)
        }

        fun håndter(innsending: Innsending, eksisterendeSak: EksisterendesakData) {
            eksisterendeSak.warn("Forventet ikke Eksisterendesak i %s", type.name)
        }

        fun håndter(innsending: Innsending, arenaOppgave: ArenaOppgaveOpprettet) {
            arenaOppgave.warn("Forventet ikke ArenaOppgaveOpprettet i %s", type.name)
        }

        fun håndter(innsending: Innsending, gosysOppgave: GosysOppgaveOpprettet) {
            gosysOppgave.warn("Forventet ikke GosysOppgaveOpprettet i %s", type.name)
        }

        fun håndter(innsending: Innsending, oppdatertJournalpost: JournalpostOppdatert) {
            oppdatertJournalpost.warn("Forventet ikke ArenaOppgaveOpprettet i %s", type.name)
        }

        fun håndter(
            innsending: Innsending,
            journalpostferdigstilt: no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
        ) {
            journalpostferdigstilt.warn("Forventet ikke JournalpostFerdigstilt i %s", type.name)
        }

        fun leaving(hendelse: Hendelse) {}
        fun entering(innsending: Innsending, hendelse: Hendelse) {}

        override fun toSpesifikkKontekst(): SpesifikkKontekst {
            return SpesifikkKontekst(
                "Tilstand",
                mapOf(
                    "tilstand" to type.name
                )
            )
        }
    }

    internal object Mottatt : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.MottattType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun håndter(innsending: Innsending, joarkHendelse: JoarkHendelse) {
            innsending.trengerJournalpost(joarkHendelse)
            innsending.tilstand(joarkHendelse, AvventerJournalpost)
        }
    }

    internal object AvventerJournalpost : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerJournalpostType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun håndter(innsending: Innsending, journalpostData: JournalpostData) {
            innsending.journalpostData = journalpostData
            when (requireNotNull(innsending.journalpostData).kategorisertJournalpost()) {
                is UtenBruker -> {
                    journalpostData.warn("Journalpost uten registrert bruker")
                    innsending.tilstand(journalpostData, Kategorisering)
                }
                else -> innsending.tilstand(journalpostData, AvventerPersondata)
            }
        }
    }

    internal object AvventerPersondata : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerPersondataType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.trengerPersonData(hendelse)
        }

        override fun håndter(innsending: Innsending, personInformasjon: PersonInformasjon) {
            innsending.person = personInformasjon.person()
            innsending.tilstand(personInformasjon, Kategorisering)
        }
    }

    internal object Kategorisering : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.KategoriseringType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            val journalpostData = requireNotNull(innsending.journalpostData) { " Journalpost må være innhentet " }
            val hendelseType = journalpostData.javaClass.simpleName
            hendelse.info("Kategorisert journalpost til $hendelseType ")
            when (journalpostData.kategorisertJournalpost()) {
                is NySøknad -> innsending.tilstand(hendelse, AvventerSøknadsdata)
                is Gjenopptak -> innsending.tilstand(hendelse, AvventerSøknadsdata)
                is Utdanning -> innsending.tilstand(hendelse, AventerArenaOppgave)
                is Etablering -> innsending.tilstand(hendelse, AventerArenaOppgave)
                is KlageOgAnke -> innsending.tilstand(hendelse, AventerArenaOppgave)
                is KlageOgAnkeLønnskompensasjon -> innsending.tilstand(hendelse, AvventerGosysOppgave)
                is Ettersending -> innsending.tilstand(hendelse, AventerArenaOppgave)
                is UkjentSkjemaKode -> innsending.tilstand(hendelse, AvventerGosysOppgave)
                is UtenBruker -> innsending.tilstand(hendelse, AvventerGosysOppgave)
            }
        }
    }

    internal object AvventerSøknadsdata : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerSøknadsdataType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.trengerSøknadsdata(hendelse)
        }

        override fun håndter(innsending: Innsending, søknadsdata: no.nav.dagpenger.mottak.meldinger.Søknadsdata) {
            val kategorisertJournalpost =
                requireNotNull(
                    innsending.journalpostData,
                    { " Journalpost må være kategorisert på dette tidspunktet " }
                ).kategorisertJournalpost()
            søknadsdata.info("Fikk Søknadsdata for ${kategorisertJournalpost.javaClass.name}")
            innsending.søknad = søknadsdata.søknad()
            when (kategorisertJournalpost) {
                is NySøknad -> innsending.tilstand(søknadsdata, AventerMinsteinntektVurdering)
                is Gjenopptak -> innsending.tilstand(søknadsdata, AventerArenaOppgave)
                else -> søknadsdata.severe("Forventet kun søknadsdata for NySøknad og Gjenopptak")
            }
        }
    }

    internal object AventerMinsteinntektVurdering : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerMinsteinntektVurderingType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.trengerMinsteinntektVurdering(hendelse)
        }

        override fun håndter(innsending: Innsending, vurderminsteinntektData: MinsteinntektVurderingData) {
            vurderminsteinntektData.info("Fikk minsteinntekt vurdering, vurderingen er ${vurderminsteinntektData.oppfyllerMinsteArbeidsinntekt()}")
            innsending.oppfyllerMinsteArbeidsinntekt = vurderminsteinntektData.oppfyllerMinsteArbeidsinntekt()
            innsending.tilstand(vurderminsteinntektData, AvventerSvarOmEksisterendeSaker)
        }
    }

    internal object AvventerSvarOmEksisterendeSaker : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerSvarOmEksisterendeSakerType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.trengerEksisterendeSaker(hendelse)
        }

        override fun håndter(innsending: Innsending, eksisterendeSak: EksisterendesakData) {
            eksisterendeSak.info("Fikk info om eksisterende saker: ${eksisterendeSak.harEksisterendeSaker()}")
            innsending.eksisterendeSaker = eksisterendeSak.harEksisterendeSaker()
            innsending.tilstand(eksisterendeSak, AventerArenaStartVedtak)
        }
    }

    internal object AventerArenaStartVedtak : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AventerArenaStartVedtakType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.oppretteArenaStartVedtakOppgave(hendelse)
        }

        override fun håndter(innsending: Innsending, arenaOppgave: ArenaOppgaveOpprettet) {
            innsending.arenaSak = arenaOppgave.arenaSak()
            innsending.tilstand(arenaOppgave, OppdaterJournalpost)
        }
    }

    internal object AventerArenaOppgave : Tilstand {

        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AventerArenaOppgaveType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.oppretteArenaVurderHenvendelseOppgave(hendelse)
        }

        override fun håndter(innsending: Innsending, arenaOppgave: ArenaOppgaveOpprettet) {
            innsending.arenaSak = arenaOppgave.arenaSak()
            innsending.tilstand(arenaOppgave, OppdaterJournalpost)
        }
    }

    internal object AvventerGosysOppgave : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerGosysType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.opprettGosysOppgave(hendelse)
        }

        override fun håndter(innsending: Innsending, gosysOppgave: GosysOppgaveOpprettet) {
            innsending.tilstand(gosysOppgave, JournalpostFerdigstilt)
        }
    }

    internal object OppdaterJournalpost : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.OppdaterJournalpostType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.oppdatereJournalpost(hendelse)
        }

        override fun håndter(innsending: Innsending, oppdatertJournalpost: JournalpostOppdatert) {
            innsending.oppdatertJournalpost = true
            innsending.tilstand(oppdatertJournalpost, FerdigstillJournalpost)
        }
    }

    internal object FerdigstillJournalpost : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.FerdigstillJournalpostType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.ferdigstillJournalpost(hendelse)
        }

        override fun håndter(
            innsending: Innsending,
            journalpostferdigstilt: no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
        ) {
            innsending.ferdigstilt = true
            journalpostferdigstilt.info("Ferdigstilte journalpost ${innsending.journalpostId}")
            innsending.tilstand(journalpostferdigstilt, JournalpostFerdigstilt)
        }
    }

    internal object JournalpostFerdigstilt : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.JournalpostFerdigstiltType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            if (innsending.oppdatertJournalpost == false && innsending.ferdigstilt == false) {
                hendelse.severe("Forventet at journalpost var oppdatert og ferdigstilt på i tilstand $type")
            }
        }
    }

    private fun trengerSøknadsdata(hendelse: Hendelse) {
        val jp = requireNotNull(journalpostData) { " Journalpost må være satt i ${tilstand.type} " }
        hendelse.behov(
            Søknadsdata, "Trenger søknadsdata",
            mapOf(
                "dokumentInfoId" to jp.dokumenter().first().brevkode
            )
        )
    }

    private fun trengerJournalpost(hendelse: Hendelse) {
        hendelse.behov(Journalpost, "Trenger journalpost")
    }

    private fun trengerPersonData(hendelse: Hendelse) {
        hendelse.behov(Persondata, "Trenger persondata")
    }

    private fun trengerMinsteinntektVurdering(hendelse: Hendelse) {
        hendelse.behov(MinsteinntektVurdering, "Trenger vurdering av minste arbeidsinntekt")
    }

    private fun trengerEksisterendeSaker(hendelse: Hendelse) {
        hendelse.behov(EksisterendeSaker, "Trenger opplysninger om eksisterende saker")
    }

    private fun oppretteArenaStartVedtakOppgave(hendelse: Hendelse) {
        val journalpost = requireNotNull(journalpostData).kategorisertJournalpost()
        val søknad = requireNotNull(søknad)
        val oppgavebenk = journalpost.oppgaveBenk(person, Søknad.fromJson(søknad.data), oppfyllerMinsteArbeidsinntekt)
        val parametre = mapOf(
            "fødselsnummer" to "personen!",
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to oppgavebenk.datoRegistrert,
            "tilleggsinformasjon" to oppgavebenk.tilleggsinformasjon
        )
        hendelse.behov(OpprettStartVedtakOppgave, "Oppretter oppgave og sak for journalpost $journalpostId", parametre)
    }

    private fun oppretteArenaVurderHenvendelseOppgave(
        hendelse: Hendelse
    ) {
        val journalpost = requireNotNull(journalpostData).kategorisertJournalpost()
        val oppgavebenk = journalpost.oppgaveBenk(person)
        val parametre = mapOf(
            "fødselsnummer" to "personen!",
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to oppgavebenk.datoRegistrert,
            "tilleggsinformasjon" to oppgavebenk.tilleggsinformasjon
        )

        hendelse.behov(
            OpprettVurderhenvendelseOppgave,
            "Oppretter oppgave og sak for journalpost $journalpostId",
            parametre
        )
    }

    private fun oppdatereJournalpost(hendelse: Hendelse) {
        val parametre = this.arenaSak?.let { mapOf("fagsakId" to it) } ?: emptyMap<String, Any>()
        hendelse.behov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Oppdaterjournalpost,
            "Oppdatere journalpost for $journalpostId",
            parametre
        )
    }

    private fun ferdigstillJournalpost(hendelse: Hendelse) {
        hendelse.behov(
            Ferdigstill, "Ferdigstiller journalpost $journalpostId"
        )
    }

    private fun opprettGosysOppgave(hendelse: Hendelse) {
        val journalpost = requireNotNull(journalpostData).kategorisertJournalpost()
        val oppgavebenk = journalpost.oppgaveBenk(person)

        // TODO — finn ut av parametre til gosys
        val parametre = mapOf(
            "fødselsnummer" to "personen!",
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to oppgavebenk.datoRegistrert,
            "tilleggsinformasjon" to oppgavebenk.tilleggsinformasjon
        )

        hendelse.behov(
            Gosysoppgave, "Oppretter gosysoppgave for journalpost $journalpostId",
            parametre
        )
    }

    private fun tilstand(
        event: Hendelse,
        nyTilstand: Tilstand,
        block: () -> Unit = {}
    ) {
        if (tilstand == nyTilstand) {
            return // Already in this state => ignore
        }
        tilstand.leaving(event)
        val previousState = tilstand
        tilstand = nyTilstand
        block()
        event.kontekst(tilstand)
        emitTilstandEndret(tilstand.type, event.aktivitetslogg, previousState.type, tilstand.timeout)
        tilstand.entering(this, event)
    }

    private fun emitTilstandEndret(
        gjeldendeTilstand: InnsendingTilstandType,
        aktivitetslogg: Aktivitetslogg,
        forrigeTilstand: InnsendingTilstandType,
        timeout: Duration
    ) {

        observers.forEach {
            it.tilstandEndret(
                InnsendingObserver.InnsendingEndretTilstandEvent(
                    journalpostId = journalpostId,
                    gjeldendeTilstand = gjeldendeTilstand,
                    forrigeTilstand = forrigeTilstand,
                    aktivitetslogg = aktivitetslogg,
                    timeout = timeout

                )
            )
        }
    }

    internal fun accept(visitor: InnsendingVisitor) {
        visitor.preVisitInnsending(this, journalpostId)
        visitor.visitTilstand(tilstand)
        visitor.visitInnsendingAktivitetslogg(aktivitetslogg)
        aktivitetslogg.accept(visitor)
        visitor.postVisitInnsending(this, journalpostId)
    }

    fun addObserver(observer: InnsendingObserver) {
        observers.add(observer)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst(
        "Innsending",
        mapOf(
            "journalpostId" to journalpostId
        )
    )
}
