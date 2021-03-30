package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Journalpost
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.NySøknad
import java.util.UUID

class Innsending private constructor(
    private val id: UUID,
    private val journalpostId: String,
    private var tilstand: Tilstand
) : Aktivitetskontekst {
    internal constructor(
        id: UUID,
        journalpostId: String
    ) : this(
        id = id,
        journalpostId = journalpostId,
        tilstand = Mottatt
    )

    fun journalpostId(): String = journalpostId

    interface Tilstand : Aktivitetskontekst {

        val type: InnsendingTilstandType

        fun håndter(innsending: Innsending, joarkHendelse: JoarkHendelse) {
            joarkHendelse.warn("Forventet ikke JoarkHendelse i %s", type.name)
        }

        fun håndter(innsending: Innsending, nySøknad: NySøknad) {
            nySøknad.warn("Forventet ikke NySøknad i %s", type.name)
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

        override fun håndter(innsending: Innsending, nySøknad: NySøknad) {
            innsending.trengerPersonData(nySøknad)
            innsending.tilstand(nySøknad, AvventerPersondata)
        }
    }

    internal object AvventerPersondata : Tilstand {
        override val type: InnsendingTilstandType
            get() = InnsendingTilstandType.AvventerPersondata
    }

    fun håndter(joarkHendelse: JoarkHendelse) {
        joarkHendelse.kontekst(this)
        tilstand.håndter(this, joarkHendelse)
    }

    fun håndter(nySøknad: NySøknad) {
        nySøknad.kontekst(this)
        tilstand.håndter(this, nySøknad)
    }

    private fun trengerJournalpost(hendelse: Hendelse) {
        hendelse.behov(Journalpost, "Trenger journalpost")
    }

    private fun trengerPersonData(nySøknad: NySøknad) {
        nySøknad.behov(Persondata, "Trenger persondata")
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

    fun accept(visitor: InnsendingVisitor) {
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
