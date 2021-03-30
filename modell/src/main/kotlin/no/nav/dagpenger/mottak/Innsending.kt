package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.MinsteinntektVurdering
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
    private var søknad: no.nav.dagpenger.mottak.meldinger.Søknadsdata.Søknad?
) : Aktivitetskontekst {

    internal constructor(
        id: UUID,
        journalpostId: String
    ) : this(
        id = id,
        journalpostId = journalpostId,
        tilstand = Mottatt,
        kategorisertJournalpost = null,
        søknad = null
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
}
