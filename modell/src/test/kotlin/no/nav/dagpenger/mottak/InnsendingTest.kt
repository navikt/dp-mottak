package no.nav.dagpener.mottak

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpener.mottak.meldinger.EksisterendesakData
import no.nav.dagpener.mottak.meldinger.MinsteinntektVurderingData
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingVisitor
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.JournalpostData
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class InnsendingTest {

    companion object {
        val mapper: ObjectMapper = jacksonObjectMapper()
    }

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

        assertEquals(InnsendingTilstandType.AvventerPersondata, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val persondata = PersonInformasjon(
            journalpostId = journalpostId,
            aktørId = "1234",
            fødselsnummer = "12345678901",
            norskTilknytning = true
        )

        innsending.håndter(persondata)

        assertEquals(InnsendingTilstandType.AvventerSøknadsdata, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val søknadsdata = Søknadsdata(
            journalpostId = journalpostId,
            søknadsId = "12233#",
            data = mapper.createObjectNode().also { it.put("data", "data") }
        )

        innsending.håndter(søknadsdata)

        assertEquals(InnsendingTilstandType.AvventerMinsteinntektVurdering, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val vurderminsteinntektData = MinsteinntektVurderingData(
            journalpostId = journalpostId,
            oppfyllerMinsteArbeidsinntekt = false
        )

        innsending.håndter(vurderminsteinntektData)

        assertEquals(InnsendingTilstandType.AvventerSvarOmEksisterendeSaker, TestInnsendingInspektør(innsending).gjeldendetilstand)

        val eksisterendeSak = EksisterendesakData(
            journalpostId = journalpostId,
            harEksisterendeSak = false
        )

        innsending.håndter(eksisterendeSak)

        assertEquals(InnsendingTilstandType.AventerArenaStartVedtak, TestInnsendingInspektør(innsending).gjeldendetilstand)
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
