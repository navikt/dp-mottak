package no.nav.dagpenger.mottak

import no.nav.dagpener.mottak.meldinger.EksisterendesakData
import no.nav.dagpener.mottak.meldinger.MinsteinntektVurderingData
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.EksisterendeSaker
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.MinsteinntektVurdering
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettStartVedtakOppgave
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Søknadsdata
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.JournalpostData.KategorisertJournalpost
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import java.util.UUID

class Innsending private constructor(
    private val id: UUID,
    private val journalpostId: String,
    private var tilstand: Tilstand,
    private var kategorisertJournalpost: KategorisertJournalpost?,
    private var søknad: no.nav.dagpenger.mottak.meldinger.Søknadsdata.Søknad?,
    private var oppfyllerMinsteArbeidsinntekt: Boolean?,
    private var eksisterendeSaker: Boolean?
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
        eksisterendeSaker = null
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
            get() = InnsendingTilstandType.Mottatt

        override fun håndter(innsending: Innsending, joarkHendelse: JoarkHendelse) {
            innsending.trengerJournalpost(joarkHendelse)
            innsending.tilstand(joarkHendelse, AvventerJournalpost)
        }
    }

    internal object AvventerJournalpost : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerJournalpost

        override fun håndter(innsending: Innsending, journalpostData: JournalpostData) {
            innsending.kategorisertJournalpost = journalpostData.journalpost()
            innsending.trengerPersonData(journalpostData)
            innsending.tilstand(journalpostData, AvventerPersondata)
        }
    }

    internal object AvventerPersondata : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerPersondata

        override fun håndter(innsending: Innsending, personInformasjon: PersonInformasjon) {
            innsending.tilstand(personInformasjon, JournalpostKategorisering)
        }
    }

    internal object JournalpostKategorisering : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.Kategorisering

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
            get() = InnsendingTilstandType.AvventerSøknadsdata

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
            get() = InnsendingTilstandType.AvventerMinsteinntektVurdering

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
            get() = InnsendingTilstandType.AvventerSvarOmEksisterendeSaker

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
            get() = InnsendingTilstandType.AventerArenaStartVedtak

        override fun entering(innsending: Innsending, event: Hendelse) {
            innsending.oppretteArenaStartVedtak(event, innsending.oppgaveBeskrivelseOgBenk())
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
        val parametre = mapOf(
            "fødselsnummer" to "personen!",
            "behandlendeEnhetId" to oppgavebenk.id,
            "oppgavebeskrivelse" to oppgavebenk.beskrivelse,
            "registrertDato" to requireNotNull(kategorisertJournalpost).datoRegistrert()
        )
        hendelse.behov(OpprettStartVedtakOppgave, "Trenger opplysninger om eksisterende saker", parametre)
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
