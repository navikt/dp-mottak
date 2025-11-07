package no.nav.dagpenger.mottak.e2e

import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Aktivitetslogg.Aktivitet.Behov.Behovtype
import no.nav.dagpenger.mottak.Fagsystem
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import no.nav.dagpenger.mottak.ReplayFerdigstillEvent
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.DagpengerOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.GosysOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.HåndtertInnsending
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.Journalpost.Bruker
import no.nav.dagpenger.mottak.meldinger.JournalpostFerdigstilt
import no.nav.dagpenger.mottak.meldinger.JournalpostOppdatert
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.dagpenger.mottak.meldinger.søknadsdata.Søknadsdata
import no.nav.dagpenger.mottak.meldinger.utenSeksjoner
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import java.time.LocalDateTime
import java.util.UUID

abstract class AbstractEndeTilEndeTest {
    protected companion object {
        private const val NAVN = "TEST TESTEN"
        private const val AKTØRID = "42"
        private const val JOURNALPOST_ID = "12345"
        const val ARENA_FAGSAK_ID = "9867541"
        const val ARENA_OPPGAVE_ID = "1234"
        const val DAGPENGER_FAGSAK_ID = "a707fc07-4691-46ea-82f7-52a53b4a4786"
        const val DAGPENGER_OPPGAVE_ID = "a707fc07-4691-46ea-82f7-52a53b4a4786"
    }

    protected lateinit var innsending: Innsending
    protected lateinit var observatør: TestObservatør
    protected lateinit var plantUmlObservatør: PlantUmlObservatør
    protected val inspektør get() = TestInnsendingInspektør(innsending)

    @BeforeEach
    internal fun setup() {
        innsending = Innsending(JOURNALPOST_ID)
        observatør =
            TestObservatør().also {
                innsending.addObserver(it)
            }
        plantUmlObservatør =
            PlantUmlObservatør().also {
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

    protected fun assertBehovDetaljer(
        type: Behovtype,
        detaljer: Set<String> = emptySet(),
    ) {
        val behov =
            inspektør.innsendingLogg.behov().find { behov ->
                behov.type == type
            } ?: throw AssertionError("Fant ikke behov ${type.name} i etterspurte behov")

        val forventet = detaljer + setOf("tilstand", "journalpostId")
        val faktisk = behov.detaljer().keys + behov.kontekster.flatMap { it.kontekstMap.keys }
        faktisk.shouldContainExactlyInAnyOrder(forventet)
    }

    protected fun assertFerdigstilt(test: (InnsendingObserver.InnsendingEvent) -> Unit) {
        assertNotNull(observatør.event)
        assertEquals(JOURNALPOST_ID, observatør.event?.journalpostId)
        test(observatør.event!!)
    }

    protected fun assertPuml(brevkode: String) {
        plantUmlObservatør.verify(brevkode)
    }

    protected fun assertMottatt(test: (InnsendingObserver.InnsendingEvent) -> Unit) {
        assertNotNull(observatør.mottattEvent)
        assertEquals(JOURNALPOST_ID, observatør.mottattEvent?.journalpostId)
        test(observatør.mottattEvent!!)
    }

    protected fun håndterJoarkHendelse() {
        innsending.håndter(joarkhendelse())
    }

    protected fun hånderReplayFerdigstilt() {
        innsending.håndter(ReplayFerdigstillEvent(JOURNALPOST_ID))
    }

    protected fun håndterJournalpostData(
        brevkode: String = "NAV 04-01.03",
        behandlingstema: String? = null,
        bruker: Bruker? = Bruker(id = "1234", type = Journalpost.BrukerType.AKTOERID),
        journalpostStatus: String = "MOTTATT",
    ) {
        innsending.håndter(
            journalpostData(
                brevkode = brevkode,
                behandlingstema = behandlingstema,
                bruker = bruker,
                journalpostStatus = journalpostStatus,
            ),
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

    protected fun håndterInnsending(fagsystem: Fagsystem.FagsystemType) {
        innsending.håndter(innsendingHåndtert(fagsystem))
    }

    protected fun håndterArenaOppgaveOpprettet() {
        innsending.håndter(arenaOppgaveOpprettet())
    }

    protected fun håndterDagpengerOppgaveOpprettet() {
        innsending.håndter(dagpengerOppgaveOpprettet())
    }

    protected fun håndterGosysOppgaveOpprettet() {
        innsending.håndter(gosysOppgaveOpprettet())
    }

    protected fun håndterJournalpostOppdatert() {
        innsending.håndter(journalpostOppdatert())
    }

    protected fun håndterOppgaveOpprettet() {
        innsending.håndter(oppgaveOpprettet())
    }

    protected fun håndterJournalpostFerdigstilt() {
        innsending.håndter(journalpostFerdigstilt())
    }

    private fun journalpostFerdigstilt(): JournalpostFerdigstilt =
        JournalpostFerdigstilt(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
        )

    private fun journalpostOppdatert(): JournalpostOppdatert =
        JournalpostOppdatert(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
        )

    private fun arenaOppgaveOpprettet(): ArenaOppgaveOpprettet =
        ArenaOppgaveOpprettet(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            oppgaveId = ARENA_OPPGAVE_ID,
            fagsakId = ARENA_FAGSAK_ID,
        )

    private fun dagpengerOppgaveOpprettet(): DagpengerOppgaveOpprettet =
        DagpengerOppgaveOpprettet(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            oppgaveId = UUID.fromString(DAGPENGER_OPPGAVE_ID),
            fagsakId = UUID.fromString(DAGPENGER_FAGSAK_ID),
        )

    private fun innsendingHåndtert(fagsystemType: Fagsystem.FagsystemType): HåndtertInnsending {
        val fagsystem =
            when (fagsystemType) {
                Fagsystem.FagsystemType.DAGPENGER -> Fagsystem.Dagpenger(sakId = UUID.fromString(DAGPENGER_FAGSAK_ID))
                Fagsystem.FagsystemType.ARENA -> Fagsystem.Arena
            }

        return HåndtertInnsending(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            fagsystem = fagsystem,
        )
    }

    private fun oppgaveOpprettet(): DagpengerOppgaveOpprettet =
        DagpengerOppgaveOpprettet(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            oppgaveId = UUID.randomUUID(),
            fagsakId = UUID.randomUUID(),
        )

    private fun gosysOppgaveOpprettet(): GosysOppgaveOpprettet =
        GosysOppgaveOpprettet(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            oppgaveId = "1234567",
        )

    private fun søknadsdata(): Søknadsdata =
        Søknadsdata(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            data = utenSeksjoner(),
        )

    private fun personInformasjon(): PersonInformasjon =
        PersonInformasjon(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            aktørId = AKTØRID,
            ident = GENERERT_FØDSELSNUMMER,
            norskTilknytning = true,
            navn = NAVN,
        )

    private fun personInformasjonIkkeFunnet(): PersonInformasjonIkkeFunnet =
        PersonInformasjonIkkeFunnet(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
        )

    private fun journalpostData(
        brevkode: String,
        behandlingstema: String? = null,
        bruker: Bruker?,
        journalpostStatus: String,
    ): Journalpost =
        Journalpost(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            journalpostStatus = journalpostStatus,
            bruker = bruker,
            behandlingstema = behandlingstema,
            registrertDato = LocalDateTime.now(),
            dokumenter =
                listOf(
                    Journalpost.DokumentInfo(
                        tittelHvisTilgjengelig = null,
                        dokumentInfoId = "123",
                        brevkode = brevkode,
                        hovedDokument = true,
                    ),
                ),
        )

    private fun joarkhendelse(): JoarkHendelse =
        JoarkHendelse(
            aktivitetslogg = Aktivitetslogg(),
            journalpostId = JOURNALPOST_ID,
            hendelseType = "MIDLERTIDIG",
            journalpostStatus = "MOTTATT",
            mottakskanal = "NAV_NO",
        )
}
