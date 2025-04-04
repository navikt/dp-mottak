package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostDokarkiv
import no.nav.dagpenger.mottak.db.ArenaOppgave
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import org.junit.jupiter.api.Test
import java.util.UUID

class
VedtakFattetMottakTest {
    private val søknadId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val arenaFagsakId = "12342"
    private val dagpengerFagsakId = UUID.randomUUID()
    private val testPersonIdent = "12345678901"
    private val nyJournalpostId = 12
    private val testOppgaver =
        listOf(
            ArenaOppgave(
                "1",
                "søknad1",
                arenaFagsakId,
                1,
            ),
            ArenaOppgave(
                "2",
                "ettersending1",
                null,
                2,
            ),
            ArenaOppgave(
                "3",
                "ettersending2",
                null,
                3,
            ),
        )

    private val testRapid = TestRapid()

    private val innsendingMetadataRepository =
        mockk<InnsendingMetadataRepository>().also {
            every {
                it.hentArenaOppgaver(
                    søknadId = søknadId,
                    ident = testPersonIdent,
                )
            } returns testOppgaver

            every {
                it.opprettKoblingTilNyJournalpostForSak(
                    jounalpostId = any(),
                    innsendingId = any(),
                    fagsakId = any(),
                )
            } just Runs
        }

    private val journalpostDokarkiv =
        mockk<JournalpostDokarkiv>().also {
            coEvery {
                it.knyttJounalPostTilNySak(any(), any(), any())
            } returns nyJournalpostId.toString()
        }

    init {
        VedtakFattetMottak(
            rapidsConnection = testRapid,
            innsendingMetadataRepository = innsendingMetadataRepository,
            journalpostDokarkiv = journalpostDokarkiv,
        )
    }

    @Test
    fun `Skal sende ut behov for sletting av Arena-oppgaver når vedtak er fattet i fagsystem Dagpenger`() {
        testRapid.sendTestMessage(vedtakFattetIDagpengerJson)
        testRapid.inspektør.let { inspektør ->
            inspektør.size shouldBe 1
            inspektør.key(0) shouldBe testPersonIdent
            inspektør.message(0).let { message ->
                message["@event_name"].asText() shouldBe "behov"
                message["@behov"].map { it.asText() }.single() shouldBe "slett_arena_oppgaver"
                message["arenaFagsakId"].asText() shouldBe arenaFagsakId
                message["behandlingId"].asUUID() shouldBe behandlingId
                message["oppgaveIder"].map { it.asText() } shouldBe listOf("søknad1", "ettersending1", "ettersending2")
            }
        }
        coVerify(exactly = 1) {
            journalpostDokarkiv.knyttJounalPostTilNySak("1", dagpengerFagsakId.toString(), testPersonIdent)
            journalpostDokarkiv.knyttJounalPostTilNySak("2", dagpengerFagsakId.toString(), testPersonIdent)
            journalpostDokarkiv.knyttJounalPostTilNySak("3", dagpengerFagsakId.toString(), testPersonIdent)
        }

        verify {
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(nyJournalpostId, 1, dagpengerFagsakId)
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(nyJournalpostId, 2, dagpengerFagsakId)
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(nyJournalpostId, 3, dagpengerFagsakId)
        }
    }

    @Test
    fun `Skal ikke sende ut behov for sletting av Arena-oppgaver når vedtak er fattet i fagsystem Arena`() {
        testRapid.sendTestMessage(vedtakFattetIArenaJson)
        testRapid.inspektør.size shouldBe 0
    }

    private val vedtakFattetIDagpengerJson =
        """
        {
            "@event_name": "vedtak_fattet",
            "ident": "$testPersonIdent",
            "behandletHendelse": {
              "datatype": "UUID",
              "id": "$søknadId",
              "type": "Søknad"
            },
            "behandlingId": "$behandlingId",
            "fagsakId": "$dagpengerFagsakId",
            "fagsystem": "Dagpenger",
            "automatisk": true
        }
        """.trimIndent()

    private val vedtakFattetIArenaJson =
        """
        {
            "@event_name": "vedtak_fattet",
            "ident": "$testPersonIdent",
            "behandletHendelse": {
              "datatype": "UUID",
              "id": "$søknadId",
              "type": "Søknad"
            },
            "behandlingId": "$behandlingId",
            "fagsakId": "$arenaFagsakId",
            "fagsystem": "Arena",
            "automatisk": true
        }
        """.trimIndent()
}
