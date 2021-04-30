package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet.ArenaSak
import no.nav.dagpenger.mottak.meldinger.Eksisterendesaker
import no.nav.dagpenger.mottak.meldinger.GosysOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.MinsteinntektArbeidsinntektVurdert
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon.Person
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import java.time.Duration

class Innsending private constructor(
    private val journalpostId: String,
    private var tilstand: Tilstand,
    private var journalpost: Journalpost?,
    private var søknad: Søknadsdata.Søknad?,
    private var oppfyllerMinsteArbeidsinntekt: Boolean?,
    private var eksisterendeSaker: Boolean?,
    private var person: Person?,
    private var arenaSak: ArenaSak?,
    internal val aktivitetslogg: Aktivitetslogg
) : Aktivitetskontekst {

    private val observers = mutableSetOf<InnsendingObserver>()

    constructor(
        journalpostId: String
    ) : this(
        journalpostId = journalpostId,
        tilstand = Mottatt,
        journalpost = null,
        søknad = null,
        oppfyllerMinsteArbeidsinntekt = null,
        eksisterendeSaker = null,
        person = null,
        arenaSak = null,
        aktivitetslogg = Aktivitetslogg()
    )

    fun journalpostId(): String = journalpostId

    fun håndter(joarkHendelse: JoarkHendelse) {
        if (journalpostId != joarkHendelse.journalpostId()) return
        kontekst(joarkHendelse, "Registrert joark hendelse")
        tilstand.håndter(this, joarkHendelse)
    }

    fun håndter(journalpost: Journalpost) {
        if (journalpostId != journalpost.journalpostId()) return
        kontekst(journalpost, "Mottatt informasjon om journalpost")
        tilstand.håndter(this, journalpost)
    }

    fun håndter(personInformasjon: PersonInformasjon) {
        if (journalpostId != personInformasjon.journalpostId()) return
        kontekst(personInformasjon, "Mottatt informasjon om person")
        tilstand.håndter(this, personInformasjon)
    }

    fun håndter(personInformasjonIkkeFunnet: PersonInformasjonIkkeFunnet) {
        if (journalpostId != personInformasjonIkkeFunnet.journalpostId()) return
        kontekst(personInformasjonIkkeFunnet, "Mottatt informasjon om person ikke funnet")
        tilstand.håndter(this, personInformasjonIkkeFunnet)
    }

    fun håndter(søknadsdata: Søknadsdata) {
        if (journalpostId != søknadsdata.journalpostId()) return
        kontekst(søknadsdata, "Mottatt søknadsdata")
        tilstand.håndter(this, søknadsdata)
    }

    fun håndter(vurderminsteinntektData: MinsteinntektArbeidsinntektVurdert) {
        if (journalpostId != vurderminsteinntektData.journalpostId()) return
        kontekst(vurderminsteinntektData, "Mottatt informasjon vurdering om minste arbeidsinntekt")
        tilstand.håndter(this, vurderminsteinntektData)
    }

    fun håndter(eksisterendeSak: Eksisterendesaker) {
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

    // Gang of four State pattern
    interface Tilstand : Aktivitetskontekst {

        val type: InnsendingTilstandType
        val timeout: Duration

        fun håndter(innsending: Innsending, joarkHendelse: JoarkHendelse) {
            joarkHendelse.warn("Forventet ikke JoarkHendelse i %s", type.name)
        }

        fun håndter(innsending: Innsending, journalpost: Journalpost) {
            journalpost.warn("Forventet ikke JournalpostData i %s", type.name)
        }

        fun håndter(innsending: Innsending, personInformasjon: PersonInformasjon) {
            personInformasjon.warn("Forventet ikke PersonInformasjon i %s", type.name)
        }

        fun håndter(innsending: Innsending, personInformasjonIkkeFunnet: PersonInformasjonIkkeFunnet) {
            personInformasjonIkkeFunnet.warn("Forventet ikke PersonInformasjonIkkeFunnet i %s", type.name)
        }

        fun håndter(innsending: Innsending, søknadsdata: Søknadsdata) {
            søknadsdata.warn("Forventet ikke Søknadsdata i %s", type.name)
        }

        fun håndter(innsending: Innsending, vurderminsteinntektData: MinsteinntektArbeidsinntektVurdert) {
            vurderminsteinntektData.warn("Forventet ikke MinsteinntektVurderingData i %s", type.name)
        }

        fun håndter(innsending: Innsending, eksisterendeSak: Eksisterendesaker) {
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
            journalpostferdigstilt.warn("Forventet ikke FerdigStilt i %s", type.name)
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

        override fun håndter(innsending: Innsending, journalpost: Journalpost) {
            innsending.journalpost = journalpost
            when (requireNotNull(innsending.journalpost).kategorisertJournalpost()) {
                is UtenBruker -> {
                    journalpost.warn("Journalpost uten registrert bruker")
                    innsending.tilstand(journalpost, Kategorisering)
                }
                else -> innsending.tilstand(journalpost, AvventerPersondata)
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

        override fun håndter(innsending: Innsending, personInformasjonIkkeFunnet: PersonInformasjonIkkeFunnet) {
            innsending.tilstand(personInformasjonIkkeFunnet, AvventerGosysOppgave)
        }
    }

    internal object Kategorisering : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.KategoriseringType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            val journalpostData = requireNotNull(innsending.journalpost) { " Journalpost må være innhentet " }
            val hendelseType = journalpostData.kategorisertJournalpost()
            hendelse.info("Kategorisert journalpost til ${hendelseType.javaClass.simpleName}")
            when (hendelseType) {
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

        override fun håndter(innsending: Innsending, søknadsdata: Søknadsdata) {
            val kategorisertJournalpost =
                requireNotNull(
                    innsending.journalpost
                ) { " Journalpost må være kategorisert på dette tidspunktet " }.kategorisertJournalpost()
            søknadsdata.info("Fikk Søknadsdata for ${kategorisertJournalpost.javaClass.simpleName}")
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

        override fun håndter(innsending: Innsending, vurderminsteinntektData: MinsteinntektArbeidsinntektVurdert) {
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

        override fun håndter(innsending: Innsending, eksisterendeSak: Eksisterendesaker) {
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
            innsending.tilstand(gosysOppgave, FerdigStilt) //hvis person er satt, oppdater journalpost
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
            innsending.tilstand(oppdatertJournalpost, AventerFerdigstill)
        }
    }

    internal object AventerFerdigstill : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerFerdigstillJournalpostType
        override val timeout: Duration
            get() = Duration.ofDays(1)

        override fun entering(innsending: Innsending, hendelse: Hendelse) {
            innsending.ferdigstillJournalpost(hendelse)
        }

        override fun håndter(
            innsending: Innsending,
            journalpostferdigstilt: no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
        ) {
            journalpostferdigstilt.info("Ferdigstilte journalpost ${innsending.journalpostId}")
            innsending.tilstand(journalpostferdigstilt, FerdigStilt)
        }
    }

    internal object FerdigStilt : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.InnsendingFerdigstiltType
        override val timeout: Duration
            get() = Duration.ofDays(1)
    }

    private fun trengerSøknadsdata(hendelse: Hendelse) {
        val jp = requireNotNull(journalpost) { " Journalpost må være satt i ${tilstand.type} " }
        hendelse.behov(
            Behovtype.Søknadsdata, "Trenger søknadsdata",
            mapOf(
                "dokumentInfoId" to jp.dokumenter().first().dokumentInfoId
            )
        )
    }

    private fun trengerJournalpost(hendelse: Hendelse) {
        hendelse.behov(Behovtype.Journalpost, "Trenger journalpost")
    }

    private fun trengerPersonData(hendelse: Hendelse) {
        val brukerId =
            requireNotNull(journalpost?.bruker()?.id) { "Bruker må eksistere på journalpost ved behov ${Behovtype.Persondata.name}" }
        hendelse.behov(
            Behovtype.Persondata, "Trenger persondata",
            mapOf(
                "brukerId" to brukerId
            )
        )
    }

    private fun trengerMinsteinntektVurdering(hendelse: Hendelse) {
        val person =
            requireNotNull(person) { "Person må eksistere på innsending ved behov ${Behovtype.MinsteinntektVurdering.name}" }
        hendelse.behov(
            Behovtype.MinsteinntektVurdering, "Trenger vurdering av minste arbeidsinntekt",
            mapOf(
                "aktørId" to person.aktørId
            )
        )
    }

    private fun trengerEksisterendeSaker(hendelse: Hendelse) {
        val person =
            requireNotNull(person) { "Person må eksistere på innsending ved behov ${Behovtype.EksisterendeSaker.name}" }
        hendelse.behov(
            Behovtype.EksisterendeSaker, "Trenger opplysninger om eksisterende saker",
            mapOf("fnr" to person.fødselsnummer)
        )
    }

    private fun oppretteArenaStartVedtakOppgave(hendelse: Hendelse) {
        val journalpost = requireNotNull(journalpost).kategorisertJournalpost()
        val søknad = requireNotNull(søknad)
        val person = requireNotNull(person)
        val oppgavebenk = journalpost.oppgaveBenk(person, søknad, oppfyllerMinsteArbeidsinntekt)
        val parametre = mapOf(
            "fødselsnummer" to person.fødselsnummer,
            "aktørId" to person.aktørId,
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to oppgavebenk.datoRegistrert,
            "tilleggsinformasjon" to oppgavebenk.tilleggsinformasjon
        )
        hendelse.behov(
            Behovtype.OpprettStartVedtakOppgave,
            "Oppretter oppgave og sak for journalpost $journalpostId",
            parametre
        )
    }

    private fun oppretteArenaVurderHenvendelseOppgave(
        hendelse: Hendelse
    ) {
        val journalpost = requireNotNull(journalpost).kategorisertJournalpost()
        val person = requireNotNull(person)
        val oppgavebenk = journalpost.oppgaveBenk(person)
        val parametre = mapOf(
            "fødselsnummer" to person.fødselsnummer,
            "aktørId" to person.aktørId,
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to oppgavebenk.datoRegistrert,
            "tilleggsinformasjon" to oppgavebenk.tilleggsinformasjon
        )

        hendelse.behov(
            Behovtype.OpprettVurderhenvendelseOppgave,
            "Oppretter oppgave og sak for journalpost $journalpostId",
            parametre
        )
    }

    private fun oppdatereJournalpost(hendelse: Hendelse) {
        val person = requireNotNull(person)
        val arenaSak = requireNotNull(arenaSak)
        val parametre = mapOf(
            "fagsakId" to arenaSak.fagsakId,
            "aktørId" to person.aktørId,
            "fødselsnummer" to person.fødselsnummer
        )
        hendelse.behov(
            Behovtype.OppdaterJournalpost,
            "Oppdatere journalpost for $journalpostId",
            parametre
        )
    }

    private fun ferdigstillJournalpost(hendelse: Hendelse) {
        hendelse.behov(
            Behovtype.FerdigstillJournalpost, "Ferdigstiller journalpost $journalpostId"
        )
    }

    private fun opprettGosysOppgave(hendelse: Hendelse) {
        val journalpost = requireNotNull(journalpost).kategorisertJournalpost()
        val oppgavebenk = journalpost.oppgaveBenk(person)
        val person = person?.let {
            mapOf(
                "fødselsnummer" to it.fødselsnummer,
                "aktørId" to it.aktørId
            )
        } ?: emptyMap()

        // TODO — finn ut av parametre til gosys
        val parametre = mapOf(
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to oppgavebenk.datoRegistrert,
            "tilleggsinformasjon" to oppgavebenk.tilleggsinformasjon
        ) + person

        hendelse.behov(
            Behovtype.OpprettGosysoppgave, "Oppretter gosysoppgave for journalpost $journalpostId",
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

    fun accept(visitor: InnsendingVisitor) {
        visitor.preVisitInnsending(this, journalpostId)
        visitor.visitTilstand(tilstand)
        visitor.visitInnsending(oppfyllerMinsteArbeidsinntekt, eksisterendeSaker)
        journalpost?.accept(visitor)
        arenaSak?.accept(visitor)
        person?.accept(visitor)
        søknad?.accept(visitor)
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
