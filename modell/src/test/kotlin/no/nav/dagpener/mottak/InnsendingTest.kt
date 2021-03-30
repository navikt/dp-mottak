package no.nav.dagpener.mottak

import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingVisitor
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.UUID

class InnsendingTest {

    @Test
    fun `skal håndtere joark hendelse der journalpost er ny søknad`() {
        val journalpostId = "12345"
        val innsending = Innsending(
            id = UUID.randomUUID(),
            journalpostId = journalpostId
        )
        val joarkHendelse = JoarkHendelse(
            journalpostId = journalpostId,
            hendelseType = "MIDLERTIDIG",
            journalpostStatus = "MOTTATT"
        )

        innsending.håndter(joarkHendelse)

        assertFalse(joarkHendelse.behov().isEmpty())
        val behov = joarkHendelse.behov().first()
        assertEquals(Behovtype.Journalpost, behov.type)
        assertEquals(InnsendingTilstandType.AvventerJournalpost, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val nySøknad = JournalpostData(
            journalpostId = journalpostId,
            journalpostStatus = "MOTTATT",
            aktørId = "1234",
            dokumenter = listOf(JournalpostData.DokumentInfo(tittel = null, dokumentInfoId = "123", brevkode = "NAV 04-01.03"))
        )

        innsending.håndter(nySøknad)

        assertFalse(nySøknad.behov().isEmpty())
        assertEquals(Behovtype.Persondata, nySøknad.behov().first().type)

        assertEquals(InnsendingTilstandType.AvventerPersondata, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val persondata = PersonInformasjon(
            journalpostId = journalpostId,
            aktørId = "1234",
            fødselsnummer = "12345678901",
            norskTilknytning = true
        )

        innsending.håndter(persondata)

        assertEquals(InnsendingTilstandType.Kategorisering, TestInnsendingInspektør(innsending).gjeldendetilstand)

    }
}

internal class TestInnsendingInspektør(innsending: Innsending) : InnsendingVisitor {

    lateinit var gjeldendetilstand: InnsendingTilstandType

    init {
        innsending.accept(this)
    }

    override fun visitTilstand(tilstandType: Innsending.Tilstand) {
        gjeldendetilstand = tilstandType.type
    }
}
