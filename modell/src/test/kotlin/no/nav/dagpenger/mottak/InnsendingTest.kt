package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.EksisterendesakData
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.MinsteinntektVurderingData
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

internal class InnsendingTest {

    companion object {
        val mapper: ObjectMapper = jacksonObjectMapper()
    }

    @Test
    fun `skal håndtere joark hendelse der journalpost er ny søknad`() {
        val journalpostId = "12345"
        val innsending = Innsending(
            journalpostId = journalpostId
        )
        val joarkHendelse = JoarkHendelse(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId,
            hendelseType = "MIDLERTIDIG",
            journalpostStatus = "MOTTATT"
        )

        innsending.håndter(joarkHendelse)

        assertFalse(joarkHendelse.behov().isEmpty())
        val behov = joarkHendelse.behov().first()
        assertEquals(Behovtype.Journalpost, behov.type)
        assertEquals(InnsendingTilstandType.AvventerJournalpostType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val nySøknad = JournalpostData(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId,
            journalpostStatus = "MOTTATT",
            aktørId = "1234",
            relevanteDatoer = listOf(
                JournalpostData.RelevantDato(LocalDateTime.now().toString(), JournalpostData.Datotype.DATO_REGISTRERT)
            ),
            dokumenter = listOf(
                JournalpostData.DokumentInfo(
                    kanskjetittel = null,
                    dokumentInfoId = "123",
                    brevkode = "NAV 04-01.03"
                )
            )
        )

        innsending.håndter(nySøknad)

        assertFalse(nySøknad.behov().isEmpty())
        assertEquals(Behovtype.Persondata, nySøknad.behov().first().type)

        assertEquals(InnsendingTilstandType.AvventerPersondataType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val persondata = PersonInformasjon(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId,
            aktoerId = "1234",
            naturligIdent = "12345678901",
            norskTilknytning = true
        )

        innsending.håndter(persondata)

        assertEquals(InnsendingTilstandType.AvventerSøknadsdataType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val søknadsdata = Søknadsdata(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId,
            søknadsId = "12233#",
            data = mapper.createObjectNode().also { it.put("data", "data") }
        )

        innsending.håndter(søknadsdata)

        assertEquals(InnsendingTilstandType.AvventerMinsteinntektVurderingType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val vurderminsteinntektData = MinsteinntektVurderingData(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId,
            oppfyllerMinsteArbeidsinntekt = false
        )

        innsending.håndter(vurderminsteinntektData)

        assertEquals(InnsendingTilstandType.AvventerSvarOmEksisterendeSakerType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val eksisterendeSak = EksisterendesakData(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId,
            harEksisterendeSak = false
        )

        innsending.håndter(eksisterendeSak)

        assertEquals(InnsendingTilstandType.AventerArenaStartVedtakType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val arenaOppgave = ArenaOppgaveOpprettet(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId,
            oppgaveId = "1234",
            fagsakId = "9867541"
        )

        innsending.håndter(arenaOppgave)
        assertEquals(InnsendingTilstandType.OppdaterJournalpostType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val oppdatertJournalpost = JournalpostOppdatert(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId
        )

        innsending.håndter(oppdatertJournalpost)

        assertEquals(InnsendingTilstandType.FerdigstillJournalpostType, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val journalpostferdigstilt = JournalpostFerdigstilt(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = journalpostId
        )

        innsending.håndter(journalpostferdigstilt)

        assertEquals(InnsendingTilstandType.JournalførtType, TestInnsendingInspektør(innsending).gjeldendetilstand)
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
