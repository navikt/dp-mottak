package no.nav.dagpenger.mottak.e2e

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingVisitor
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import no.nav.dagpenger.mottak.db.InnsendingPostgresRepository
import no.nav.dagpenger.mottak.db.PostgresDataSourceBuilder
import no.nav.dagpenger.mottak.db.PostgresTestHelper.withMigratedDb
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.DagpengerOppgaveOpprettet
import no.nav.dagpenger.mottak.tjenester.MottakMediator
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertNotNull
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.time.LocalDateTime.now
import java.util.UUID
import kotlin.random.Random

internal class MediatorE2ETest {
    var journalpostId: Long = 0L

    private val testRapid = TestRapid()

    private val testObservatør = TestObservatør()

    private object TestDagpengerOppgave {
        val oppgaveId = UUID.randomUUID()
        val fagsakId = UUID.randomUUID()
    }

    private object TestArenaOppgave {
        val oppgaveId = "1231"
        val fagsakId = null
    }

    @BeforeEach
    fun setup() {
        journalpostId = Random.nextLong()
        testObservatør.reset()
    }

    @Test
    fun `Skal motta hendelse om ny søknad og sende ut behov der oppgave skal opprettes i Arena`() {
        withMigratedDb {
            settOppInfrastruktur()

            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "NAV 04-01.03"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = null, håndtert = false))
            assertBehov("OpprettStartVedtakOppgave", 4)
            håndterHendelse(opprettStartVedtakMotattHendelse())
            assertBehov("OppdaterJournalpost", 5)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 6)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 7,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta hendelse om ny søknad og sende ut behov der henvendelse skal opprettes i Dagpenger`() {
        withMigratedDb {
            val fagsakId = UUID.randomUUID()
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "NAV 04-01.03"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = fagsakId.toString(), håndtert = true))
            assertBehov("OppdaterJournalpost", 4)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 5)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 6,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta hendelse om gjenopptak og sende ut behov der oppgave skal til Arena`() {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "NAV 04-16.03"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = null, håndtert = false))
            assertBehov("OpprettVurderhenvendelseOppgave", 4)
            håndterHendelse(opprettOpprettVurderhenvendelseHendelse())
            assertBehov("OppdaterJournalpost", 5)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 6)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 7,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta hendelse om gjenopptak og sende ut behov der henvendelse skal opprettes i Dagpenger`() {
        withMigratedDb {
            val fagsakId = UUID.randomUUID()
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "NAV 04-16.03"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = fagsakId.toString(), håndtert = true))
            assertBehov("OppdaterJournalpost", 4)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 5)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 6,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta hendelser om ny søknad der en ikke kan opprette oppgave i Arena`() {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "NAV 04-01.03"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = null, håndtert = false))
            assertBehov("OpprettStartVedtakOppgave", 4)
            håndterHendelse(opprettArenaOppgaveFeilet())
            assertBehov("OpprettGosysoppgave", 5)
            håndterHendelse(gosysOppgaveOpprettetHendelse())
            assertBehov("OppdaterJournalpost", 6)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 7,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta ukjent skjemakode som fører til manuell behandling i Gosys`() {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "ukjent"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("HåndterHenvendelse", 2)
            håndterHendelse(håndtertHenvendelse(sakId = null, håndtert = false))
            assertBehov("OpprettGosysoppgave", 3)
            håndterHendelse(gosysOppgaveOpprettetHendelse())
            assertBehov("OppdaterJournalpost", 4)
            håndterHendelse(oppdatertJournalpostMotattHendelse())

            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta ukjent skjemakode som fører til henvendelse i Dagpenger`() {
        val sakId = UUID.randomUUID()
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "ukjent"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("HåndterHenvendelse", 2)
            håndterHendelse(håndtertHenvendelse(sakId = sakId.toString(), håndtert = true))
            assertBehov("OppdaterJournalpost", 3)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 4)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())

            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta hendelser som allerede er behandlet`() {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "ukjent", journalstatus = "JOURNALFOERT"))
            assertEquals(InnsendingTilstandType.AlleredeBehandletType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta generell henvendelse som skal til Dagpenger`() {
        val fagsakId = UUID.randomUUID()
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "GENERELL_INNSENDING"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = fagsakId.toString(), håndtert = true))
            assertBehov("OppdaterJournalpost", 4)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 5)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 6,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @Test
    fun `Skal motta generell henvendelse som skal til Arena`() {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = "GENERELL_INNSENDING"))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = null, håndtert = false))
            assertBehov("OpprettVurderhenvendelseOppgave", 4)
            håndterHendelse(opprettOpprettVurderhenvendelseHendelse())
            assertBehov("OppdaterJournalpost", 5)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 6)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 7,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAVe 04-01.03", "NAVe 04-01.04"])
    fun `Skal motta ettersending som skal til Dagpenger`(brevkode: String) {
        val fagsakId = UUID.randomUUID()
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = brevkode))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = fagsakId.toString(), håndtert = true))
            assertBehov("OppdaterJournalpost", 4)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 5)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())

            assertInnsending(journalpostId.toString()) { testInnsendingVisitor ->
                assertNotNull(testInnsendingVisitor.oppgaveSak)
                testInnsendingVisitor.oppgaveSak!!.fagsakId shouldBe fagsakId
            }
            assertTrue(
                testRapid.inspektør.size == 6,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAVe 04-01.03", "NAVe 04-01.04"])
    fun `Skal motta ettersending som skal til Arena`(brevkode: String) {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = brevkode))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("Søknadsdata", 2)
            håndterHendelse(søknadsdataMottakHendelse())
            assertBehov("HåndterHenvendelse", 3)
            håndterHendelse(håndtertHenvendelse(sakId = null, håndtert = false))
            assertBehov("OpprettVurderhenvendelseOppgave", 4)
            håndterHendelse(
                opprettOpprettVurderhenvendelseHendelse(
                    oppgaveId = TestArenaOppgave.oppgaveId,
                ),
            )

            assertBehov("OppdaterJournalpost", 5)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 6)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())

            assertInnsending(journalpostId.toString()) { testInnsendingVisitor ->
                assertNotNull(testInnsendingVisitor.arenaSak)
                testInnsendingVisitor.arenaSak!!.oppgaveId shouldBe TestArenaOppgave.oppgaveId
            }
            assertTrue(
                testRapid.inspektør.size == 7,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 90-00.08", "NAV 90-00.08 K", "NAVe 90-00.08 K", "NAV 90-00.08 A", "NAVe 90-00.08 A"])
    fun `Skal motta klage og anke i DAGPENGER`(brevkode: String) {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = brevkode))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("HåndterHenvendelse", 2)
            håndterHendelse(håndtertHenvendelse(sakId = TestDagpengerOppgave.fagsakId.toString(), håndtert = true))
            assertInnsending(journalpostId.toString()) { testInnsendingVisitor ->
                assertNotNull(testInnsendingVisitor.oppgaveSak)
                testInnsendingVisitor.oppgaveSak!!.fagsakId shouldBe TestDagpengerOppgave.fagsakId
            }
            assertBehov("OppdaterJournalpost", 3)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 4)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 5,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    @ParameterizedTest
    @ValueSource(strings = ["NAV 90-00.08", "NAV 90-00.08 K", "NAVe 90-00.08 K", "NAV 90-00.08 A", "NAVe 90-00.08 A"])
    fun `Skal motta klage og anke i Arena`(brevkode: String) {
        withMigratedDb {
            settOppInfrastruktur()
            håndterHendelse(joarkMelding())
            assertBehov("Journalpost", 0)
            håndterHendelse(journalpostMottattHendelse(brevkode = brevkode))
            assertBehov("Persondata", 1)
            håndterHendelse(persondataMottattHendelse())
            assertBehov("HåndterHenvendelse", 2)
            val melding = håndtertHenvendelse(sakId = TestArenaOppgave.fagsakId, håndtert = false)
            håndterHendelse(melding)
            assertBehov("OpprettVurderhenvendelseOppgave", 3)
            håndterHendelse(
                opprettOpprettVurderhenvendelseHendelse(
                    oppgaveId = TestArenaOppgave.oppgaveId,
                ),
            )
            assertInnsending(journalpostId.toString()) { testInnsendingVisitor ->
                assertNotNull(testInnsendingVisitor.arenaSak)
                testInnsendingVisitor.arenaSak!!.oppgaveId shouldBe TestArenaOppgave.oppgaveId
            }

            assertBehov("OppdaterJournalpost", 4)
            håndterHendelse(oppdatertJournalpostMotattHendelse())
            assertBehov("FerdigstillJournalpost", 5)
            håndterHendelse(ferdigstiltJournalpostMotattHendelse())
            assertTrue(
                testRapid.inspektør.size == 6,
                "For mange behov på kafka rapid, antall er : ${testRapid.inspektør.size}",
            )
            assertEquals(InnsendingTilstandType.InnsendingFerdigstiltType, testObservatør.tilstander.last())
        }
    }

    private fun assertInnsending(
        journalpostId: String,
        assertBlock: (innsending: TestInnsendingSakVisitor) -> Unit,
    ) {
        InnsendingPostgresRepository(
            PostgresDataSourceBuilder.dataSource,
        ).let {
            val innsending = requireNotNull(it.hent(journalpostId))
            assertBlock(TestInnsendingSakVisitor(innsending))
        }
    }

    private fun settOppInfrastruktur(additionalObservers: List<InnsendingObserver> = emptyList()) {
        val innsendingRepository =
            InnsendingPostgresRepository(datasource = PostgresDataSourceBuilder.dataSource).also {
                PostgresDataSourceBuilder.runMigration()
            }
        val innsendingMediator =
            InnsendingMediator(
                innsendingRepository = innsendingRepository,
                rapidsConnection = testRapid,
                observatører = listOf(testObservatør) + additionalObservers,
            )

        MottakMediator(innsendingMediator, testRapid)
    }

    private fun assertBehov(
        expectedBehov: String,
        indexPåMelding: Int,
    ) {
        assertTrue(testRapid.inspektør.size == indexPåMelding + 1, "Ingen melding på index $indexPåMelding, forventet $expectedBehov")
        testRapid.inspektør.message(indexPåMelding).also { jsonNode ->
            assertEquals(expectedBehov, jsonNode["@behov"].map { it.asText() }.first())
            assertEquals(journalpostId.toString(), jsonNode["journalpostId"].asText())
        }
    }

    private fun håndterHendelse(melding: String) {
        testRapid.sendTestMessage(melding)
    }

    private fun journalpostMottattHendelse(
        brevkode: String,
        journalstatus: String = "MOTTATT",
    ): String =
        //language=JSON
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Journalpost"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "Journalpost": {
                "id" : "$journalpostId",
                "journalstatus" : "$journalstatus",
                "bruker" : {
                  "id": "12345678901",
                  "type": "FNR"
                },
                "relevanteDatoer" : [
                    {
                      "dato" : "${now()}",
                      "datotype": "DATO_REGISTRERT"
                    }
                ],
                "dokumenter" : [
                  {
                    "tittel" : null,
                    "dokumentInfoId" : 1234,
                    "brevkode" : "$brevkode",
                    "hovedDokument" : true
                  },
                   {
                    "tittel" : null,
                    "dokumentInfoId" : 5678,
                    "brevkode" : "N6",
                    "hovedDokument" : false
                  }
                ],
                "behandlingstema" : null
            }
          }
        }
        """.trimIndent()

    private fun håndtertHenvendelse(
        sakId: String?,
        håndtert: Boolean,
    ): String {
        val fagsakIdJson = sakId?.let { """ "sakId": "$sakId",""" } ?: ""
        //language=JSON
        return """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "HåndterHenvendelse"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "HåndterHenvendelse": {
              $fagsakIdJson 
              "håndtert": $håndtert
            }
          }
        }"""
    }

    //language=JSON
    private fun persondataMottattHendelse(): String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Persondata"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "Persondata": {
              "navn": "Test Testen",
              "aktørId": "tadda",
              "fødselsnummer": "$GENERERT_FØDSELSNUMMER",
              "norskTilknytning": true,
              "diskresjonskode": null
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun joarkMelding(): String =
        """
        {
          "hendelsesId": "",
          "versjon": "",
          "hendelsesType": "MidlertidigJournalført",
          "journalpostId": "$journalpostId",
          "journalpostStatus": "Mottatt",
          "temaGammelt": "DAG",
          "temaNytt": "DAG",
          "mottaksKanal": "NAV_NO",
          "kanalReferanseId": "vetikke"
        }
        """.trimIndent()

    //language=JSON
    private fun søknadsdataMottakHendelse(): String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Søknadsdata"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "Søknadsdata": {
        "søknad_uuid": "cfd84357-cdd9-4811-ada5-63d77625e91e",
        "versjon_navn": "Dagpenger",
        "seksjoner": [ ]
              }
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun opprettArenaOppgaveFeilet(): String =
        """
        {
                  "@event_name": "behov",
                  "@final": true,
                  "@id": "${UUID.randomUUID()}",
                  "@behov": [
                    "OpprettStartVedtakOppgave"
                  ],
                  "@opprettet": "${now()}",
                  "journalpostId": "$journalpostId",
                  "@løsning": {
                    "OpprettStartVedtakOppgave": {
                      "@feil" : "Kunne ikke opprette Arena oppgave"
                    }
                  }
        }
        """.trimIndent()

    //language=JSON
    private fun opprettStartVedtakMotattHendelse(): String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettStartVedtakOppgave"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "OpprettStartVedtakOppgave": {
              "fagsakId": "12345",
              "oppgaveId": "12345678"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun opprettOpprettVurderhenvendelseHendelse(
        oppgaveId: String = "12345678",
    ): String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettVurderhenvendelseOppgave"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "OpprettVurderhenvendelseOppgave": {
              "fagsakId": "12345",
              "oppgaveId": "$oppgaveId"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun oppdatertJournalpostMotattHendelse(): String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OppdaterJournalpost"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "OppdaterJournalpost": {
              "journalpostId": "$journalpostId"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun ferdigstiltJournalpostMotattHendelse(): String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "FerdigstillJournalpost"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "FerdigstillJournalpost": {
              "journalpostId": "$journalpostId"
            }
          }
        }
        """.trimIndent()

    //language=JSON
    private fun gosysOppgaveOpprettetHendelse(): String =
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "OpprettGosysoppgave"
          ],
          "@opprettet" : "${now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "OpprettGosysoppgave": {
              "journalpostId": "$journalpostId",
              "oppgaveId": "123456"
            }
          }
        }
        """.trimIndent()

    private class TestInnsendingSakVisitor(innsending: Innsending) : InnsendingVisitor {
        var arenaSak: ArenaOppgaveOpprettet.ArenaSak? = null
        var oppgaveSak: DagpengerOppgaveOpprettet.OppgaveSak? = null

        init {
            innsending.accept(this)
        }

        override fun visitArenaSak(
            oppgaveId: String,
            fagsakId: String?,
        ) {
            this.arenaSak =
                ArenaOppgaveOpprettet.ArenaSak(
                    oppgaveId = oppgaveId,
                    fagsakId = fagsakId,
                )
        }

        override fun visitOppgaveSak(
            oppgaveId: UUID?,
            fagsakId: UUID,
        ) {
            this.oppgaveSak =
                DagpengerOppgaveOpprettet.OppgaveSak(
                    oppgaveId = oppgaveId,
                    fagsakId = fagsakId,
                )
        }
    }
}
