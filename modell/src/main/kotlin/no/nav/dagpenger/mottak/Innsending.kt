package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.EksisterendeSaker
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Ferdigstill
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.MinsteinntektVurdering
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettStartVedtakOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Søknadsdata
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet.ArenaSak
import no.nav.dagpenger.mottak.meldinger.EksisterendesakData
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.JournalpostData.KategorisertJournalpost
import no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.MinsteinntektVurderingData
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon.Person
import no.nav.dagpenger.mottak.meldinger.tilleggsinformasjon
import java.util.UUID

class Innsending private constructor(
    private val id: UUID,
    private val journalpostId: String,
    private var tilstand: Tilstand,
    private var kategorisertJournalpost: KategorisertJournalpost?,
    private var søknad: no.nav.dagpenger.mottak.meldinger.Søknadsdata.Søknad?,
    private var oppfyllerMinsteArbeidsinntekt: Boolean?,
    private var eksisterendeSaker: Boolean?,
    private var person: Person?,
    private var arenaSak: ArenaSak?,
    private var oppdatertJournalpost: Boolean?,
    private var ferdigstilt: Boolean?
) : Aktivitetskontekst {

    internal constructor(
        id: UUID,
        journalpostId: String
    ) : this(
        id = id,
        journalpostId = journalpostId,
        tilstand = Mottatt,
        kategorisertJournalpost = null,
        søknad = null,
        oppfyllerMinsteArbeidsinntekt = null,
        eksisterendeSaker = null,
        person = null,
        arenaSak = null,
        oppdatertJournalpost = null,
        ferdigstilt = null
    )

    fun journalpostId(): String = journalpostId

    fun håndter(joarkHendelse: JoarkHendelse) {
        joarkHendelse.kontekst(this)
        tilstand.håndter(this, joarkHendelse)
    }

    fun håndter(journalpostData: JournalpostData) {
        journalpostData.kontekst(this)
        tilstand.håndter(this, journalpostData)
    }

    fun håndter(personInformasjon: PersonInformasjon) {
        // @todo: må håndtere der vi rett og slett ikke får tak i person
        personInformasjon.kontekst(this)
        tilstand.håndter(this, personInformasjon)
    }

    fun håndter(søknadsdata: no.nav.dagpenger.mottak.meldinger.Søknadsdata) {
        søknadsdata.kontekst(this)
        tilstand.håndter(this, søknadsdata)
    }

    fun håndter(vurderminsteinntektData: MinsteinntektVurderingData) {
        vurderminsteinntektData.kontekst(this)
        tilstand.håndter(this, vurderminsteinntektData)
    }

    fun håndter(eksisterendeSak: EksisterendesakData) {
        eksisterendeSak.kontekst(this)
        tilstand.håndter(this, eksisterendeSak)
    }

    fun håndter(arenaOppgave: ArenaOppgaveOpprettet) {
        arenaOppgave.kontekst(this)
        tilstand.håndter(this, arenaOppgave)
    }

    fun håndter(oppdatertJournalpost: JournalpostOppdatert) {
        oppdatertJournalpost.kontekst(this)
        tilstand.håndter(this, oppdatertJournalpost)
    }

    fun håndter(journalpostferdigstilt: JournalpostFerdigstilt) {
        journalpostferdigstilt.kontekst(this)
        tilstand.håndter(this, journalpostferdigstilt)
    }

    interface Tilstand : Aktivitetskontekst {

        val type: InnsendingTilstandType

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

        fun håndter(innsending: Innsending, oppdatertJournalpost: JournalpostOppdatert) {
            oppdatertJournalpost.warn("Forventet ikke ArenaOppgaveOpprettet i %s", type.name)
        }

        fun håndter(innsending: Innsending, journalpostferdigstilt: JournalpostFerdigstilt) {
            journalpostferdigstilt.warn("Forventet ikke JournalpostFerdigstilt i %s", type.name)
        }

        fun leaving(event: Hendelse) {}
        fun entering(innsending: Innsending, event: Hendelse) {}

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

        override fun håndter(innsending: Innsending, joarkHendelse: JoarkHendelse) {
            innsending.trengerJournalpost(joarkHendelse)
            innsending.tilstand(joarkHendelse, AvventerJournalpost)
        }
    }

    internal object AvventerJournalpost : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerJournalpostType

        override fun håndter(innsending: Innsending, journalpostData: JournalpostData) {
            innsending.kategorisertJournalpost = journalpostData.journalpost()
            innsending.trengerPersonData(journalpostData)
            innsending.tilstand(journalpostData, AvventerPersondata)
        }
    }

    internal object AvventerPersondata : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerPersondataType

        override fun håndter(innsending: Innsending, personInformasjon: PersonInformasjon) {
            innsending.person = personInformasjon.person()
            innsending.tilstand(personInformasjon, JournalpostKategorisering)
        }
    }

    internal object JournalpostKategorisering : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.KategoriseringType

        override fun entering(innsending: Innsending, event: Hendelse) {
            event.info("Skal kategorisere journalpost")
            when (innsending.kategorisertJournalpost) {
                is KategorisertJournalpost.NySøknad -> innsending.tilstand(event, AvventerSøknadsdata)
                else -> TODO("IKKE KATEGORISERT ")
            }
        }
    }

    internal object AvventerSøknadsdata : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerSøknadsdataType

        override fun entering(innsending: Innsending, event: Hendelse) {
            innsending.trengerSøknadsdata(event)
        }

        override fun håndter(innsending: Innsending, søknadsdata: no.nav.dagpenger.mottak.meldinger.Søknadsdata) {
            søknadsdata.info("Fikk Søknadsdata for ${innsending.kategorisertJournalpost?.javaClass?.name}")
            innsending.søknad = søknadsdata.søknad()
            innsending.tilstand(søknadsdata, AventerMinsteinntektVurdering)
        }
    }

    internal object AventerMinsteinntektVurdering : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerMinsteinntektVurderingType

        override fun entering(innsending: Innsending, event: Hendelse) {
            innsending.trengerMinsteinntektVurdering(event)
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

        override fun entering(innsending: Innsending, event: Hendelse) {
            innsending.trengerEksisterendeSaker(event)
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

        override fun entering(innsending: Innsending, event: Hendelse) {
            innsending.oppretteArenaStartVedtak(event, innsending.oppgaveBeskrivelseOgBenk())
        }

        override fun håndter(innsending: Innsending, arenaOppgave: ArenaOppgaveOpprettet) {
            innsending.arenaSak = arenaOppgave.arenaSak()
            innsending.tilstand(arenaOppgave, OppdaterJournalpost)
        }
    }

    internal object OppdaterJournalpost : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.OppdaterJournalpostType

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

        override fun entering(innsending: Innsending, event: Hendelse) {
            innsending.ferdigstillJournalpost(event)
        }

        override fun håndter(innsending: Innsending, journalpostferdigstilt: JournalpostFerdigstilt) {
            innsending.ferdigstilt = true
            journalpostferdigstilt.info("Ferdigstilte journalpost ${innsending.journalpostId}")
            innsending.tilstand(journalpostferdigstilt, Journalført)
        }
    }

    internal object Journalført : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.JournalførtType

        override fun entering(innsending: Innsending, event: Hendelse) {
            if (innsending.oppdatertJournalpost == false && innsending.ferdigstilt == false) {
                event.severe("Forventet at journalpost var oppdatert og ferdigstilt på i tilstand $type")
            }
        }
    }

    private fun trengerSøknadsdata(hendelse: Hendelse) {
        val jp = requireNotNull(kategorisertJournalpost) { " Journalpost må være satt i ${tilstand.type} " }
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

    private fun oppretteArenaStartVedtak(hendelse: Hendelse, oppgavebenk: OppgaveBenk) {
        val journalpost = requireNotNull(kategorisertJournalpost)
        val parametre = mapOf(
            "fødselsnummer" to "personen!",
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to journalpost.datoRegistrert(),
            "tilleggsinformasjon" to journalpost.tilleggsinformasjon()
        )
        hendelse.behov(OpprettStartVedtakOppgave, "Oppretter oppgave og sak for journalpost $journalpostId", parametre)
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

    private fun tilstand(
        event: Hendelse,
        nyTilstand: Tilstand,
        block: () -> Unit = {}
    ) {
        if (tilstand == nyTilstand) {
            return // Already in this state => ignore
        }
        tilstand.leaving(event)
        tilstand = nyTilstand
        block()
        event.kontekst(tilstand)
        tilstand.entering(this, event)
    }

    internal fun accept(visitor: InnsendingVisitor) {
        visitor.preVisitInnsending(this, journalpostId)
        visitor.visitTilstand(tilstand)
        visitor.postVisitInnsending(this, journalpostId)
    }

    override fun toSpesifikkKontekst(): SpesifikkKontekst = SpesifikkKontekst(
        "Innsending",
        mapOf(
            "journalpostId" to journalpostId
        )
    )

    private data class OppgaveBenk(
        val id: String,
        val beskrivelse: String
    )

    private fun oppgaveBeskrivelseOgBenk(): OppgaveBenk {
        val kanAvslåsPåMinsteinntekt = this.oppfyllerMinsteArbeidsinntekt == false
        val søknad =
            Søknad.fromJson(requireNotNull(this.søknad) { " Søknadsdata må være satt på dette tidspunktet" }.data)
        // val koronaRegelverkMinsteinntektBrukt =
        //     packet.getNullableBoolean(PacketKeys.KORONAREGELVERK_MINSTEINNTEKT_BRUKT) == true
        val konkurs = søknad.harAvsluttetArbeidsforholdFraKonkurs()
        val grenseArbeider = søknad.erGrenseArbeider()
        val eøsArbeidsforhold = søknad.harEøsArbeidsforhold()
        val inntektFraFangstFisk = søknad.harInntektFraFangstOgFiske()
        val harAvtjentVerneplikt = søknad.harAvtjentVerneplikt()
        val erPermittertFraFiskeforedling = søknad.erPermittertFraFiskeForedling()
        //  val diskresjonskodeBenk = packet.getStringValue(PacketKeys.BEHANDLENDE_ENHET) == "2103"

        return OppgaveBenk("bla bla", "Beskrivelse")
        // return when {
        //     diskresjonskodeBenk -> OppgaveBenk(tildeltEnhetsNrFrom(packet), henvendelse(packet).oppgavebeskrivelse)
        //     eøsArbeidsforhold -> OppgaveBenk("4470", "MULIG SAMMENLEGGING - EØS\n")
        //     harAvtjentVerneplikt -> OppgaveBenk(packet.getStringValue(PacketKeys.BEHANDLENDE_ENHET), "VERNEPLIKT\n")
        //     inntektFraFangstFisk -> OppgaveBenk(
        //         packet.getStringValue(PacketKeys.BEHANDLENDE_ENHET),
        //         "FANGST OG FISKE\n"
        //     )
        //     grenseArbeider -> OppgaveBenk("4465", "EØS\n")
        //     konkurs -> OppgaveBenk("4401", "Konkurs\n")
        //     erPermittertFraFiskeforedling -> OppgaveBenk("4454", "FISK\n")
        //     kanAvslåsPåMinsteinntekt -> OppgaveBenk(
        //         packet.finnEnhetForHurtigAvslag(),
        //         if (koronaRegelverkMinsteinntektBrukt) "Minsteinntekt - mulig avslag - korona\n" else "Minsteinntekt - mulig avslag\n"
        //     )
        //     else -> OppgaveBenk(tildeltEnhetsNrFrom(packet), henvendelse(packet).oppgavebeskrivelse)
        // }
    }
}
