package no.nav.dagpenger.mottak.e2e

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.Eksisterendesaker
import no.nav.dagpenger.mottak.meldinger.GosysOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.Journalpost.Bruker
import no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.MinsteinntektArbeidsinntektVurdert
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime

abstract class AbstractEndeTilEndeTest {

    protected companion object {
        private const val NAVN = "TEST TESTEN"
        private const val UNG_PERSON_FNR_2018 = "12020052345"
        private const val AKTØRID = "42"
        private const val JOURNALPOST_ID = "12345"
        private val mapper: ObjectMapper = jacksonObjectMapper()
    }

    protected lateinit var innsending: Innsending
    protected lateinit var observatør: TestObservatør
    protected lateinit var plantUmlObservatør: PlantUmlObservatør
    protected val inspektør get() = TestInnsendingInspektør(innsending)

    @BeforeEach
    internal fun setup() {
        innsending = Innsending(JOURNALPOST_ID)
        observatør = TestObservatør().also {
            innsending.addObserver(it)
        }
        plantUmlObservatør = PlantUmlObservatør().also {
            innsending.addObserver(it)
        }
    }

    protected fun assertTilstander(vararg tilstander: InnsendingTilstandType) {
        assertEquals(tilstander.asList(), observatør.tilstander[JOURNALPOST_ID])
    }

    protected fun assertNoErrors(inspektør: TestInnsendingInspektør) {
        assertFalse(inspektør.innsendingLogg.hasErrors(), inspektør.innsendingLogg.toString())
    }

    protected fun assertMessages(inspektør: TestInnsendingInspektør) {
        assertTrue(inspektør.innsendingLogg.hasMessages(), inspektør.innsendingLogg.toString())
    }

    protected fun assertBehovDetaljer(type: Behovtype, detaljer: Set<String> = emptySet()) {

        val behov = inspektør.innsendingLogg.behov().find { behov ->
            behov.type == type
        } ?: throw AssertionError("Fant ikke behov ${type.name} i etterspurte behov")

        assertEquals(
            detaljer + setOf("tilstand", "journalpostId"),
            behov.detaljer().keys + behov.kontekster.flatMap { it.kontekstMap.keys }
        )
    }

    protected fun assertFerdigstilt(test: (InnsendingObserver.InnsendingEvent) -> Unit) {
        assertNotNull(observatør.event)
        assertEquals(JOURNALPOST_ID, observatør.event?.journalpostId)
        test(observatør.event!!)
    }

    protected fun assertMottatt(test: (InnsendingObserver.InnsendingEvent) -> Unit) {
        assertNotNull(observatør.mottattEvent)
        assertEquals(JOURNALPOST_ID, observatør.mottattEvent?.journalpostId)
        test(observatør.mottattEvent!!)
    }

    protected fun håndterJoarkHendelse() {
        innsending.håndter(joarkhendelse())
    }

    protected fun håndterJournalpostData(
        brevkode: String = "NAV 04-01.03",
        behandlingstema: String? = null,
        bruker: Bruker? = Bruker(id = "1234", type = Journalpost.BrukerType.AKTOERID),
        journalpostStatus: String = "MOTTATT"
    ) {
        innsending.håndter(
            journalpostData(
                brevkode = brevkode,
                behandlingstema = behandlingstema,
                bruker = bruker,
                journalpostStatus = journalpostStatus
            )
        )
    }

    protected fun håndterPersonInformasjon() {
        innsending.håndter(personInformasjon())
    }
    protected fun håndterPersonInformasjonIkkeFunnet() {
        innsending.håndter(personInformasjonIkkeFunnet())
    }

    protected fun håndterSøknadsdata() {
        innsending.håndter(søknadsdata())
    }

    protected fun håndterMinsteinntektVurderingData() {
        innsending.håndter(minsteinntektVurderingData())
    }

    protected fun håndterEksisterendesakData() {
        innsending.håndter(eksisterendesakData())
    }

    protected fun håndterArenaOppgaveOpprettet() {
        innsending.håndter(arenaOppgaveOpprettet())
    }

    protected fun håndterGosysOppgaveOpprettet() {
        innsending.håndter(gosysOppgaveOpprettet())
    }

    protected fun håndterJournalpostOppdatert() {
        innsending.håndter(journalpostOppdatert())
    }

    protected fun håndterJournalpostFerdigstilt() {
        innsending.håndter(journalpostFerdigstilt())
    }

    private fun journalpostFerdigstilt(): JournalpostFerdigstilt = JournalpostFerdigstilt(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID
    )

    private fun journalpostOppdatert(): JournalpostOppdatert = JournalpostOppdatert(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID
    )

    private fun arenaOppgaveOpprettet(): ArenaOppgaveOpprettet = ArenaOppgaveOpprettet(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        oppgaveId = "1234",
        fagsakId = "9867541"
    )

    private fun gosysOppgaveOpprettet(): GosysOppgaveOpprettet = GosysOppgaveOpprettet(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        oppgaveId = "1234567"
    )

    private fun eksisterendesakData(): Eksisterendesaker = Eksisterendesaker(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        harEksisterendeSak = false
    )

    private fun minsteinntektVurderingData(): MinsteinntektArbeidsinntektVurdert = MinsteinntektArbeidsinntektVurdert(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        oppfyllerMinsteArbeidsinntekt = false
    )

    private fun søknadsdata(): Søknadsdata = Søknadsdata(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        data = mapper.createObjectNode().also { it.put("data", "data") }
    )

    private fun personInformasjon(): PersonInformasjon = PersonInformasjon(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        aktørId = AKTØRID,
        fødselsnummer = UNG_PERSON_FNR_2018,
        norskTilknytning = true,
        navn = NAVN
    )
    private fun personInformasjonIkkeFunnet(): PersonInformasjonIkkeFunnet = PersonInformasjonIkkeFunnet(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID
    )

    private fun journalpostData(
        brevkode: String,
        behandlingstema: String? = null,
        bruker: Bruker?,
        journalpostStatus: String
    ): Journalpost = Journalpost(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        journalpostStatus = journalpostStatus,
        bruker = bruker,
        behandlingstema = behandlingstema,
        registrertDato = LocalDateTime.now(),
        dokumenter = listOf(
            Journalpost.DokumentInfo(
                tittelHvisTilgjengelig = null,
                dokumentInfoId = "123",
                brevkode = brevkode
            )
        )
    )

    private fun joarkhendelse(): JoarkHendelse = JoarkHendelse(
        aktivitetslogg = Aktivitetslogg(),
        journalpostId = JOURNALPOST_ID,
        hendelseType = "MIDLERTIDIG",
        journalpostStatus = "MOTTATT"
    )
}
