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
import no.nav.dagpenger.mottak.behov.journalpost.KnyttJounalPostTilNySakResponse
import no.nav.dagpenger.mottak.db.ArenaOppgave
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import no.nav.dagpenger.mottak.serder.asUUID
import org.junit.jupiter.api.Test
import java.util.UUID

class
VedtakFattetMottakTest {
    private val søknadId = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val dagpengerFagsakId = UUID.randomUUID()
    private val testPersonIdent = "12345678901"
    private val knyttJounalPostTilNySakResponse = KnyttJounalPostTilNySakResponse(12)
    private val testOppgaver =
        listOf(
            ArenaOppgave(
                "1",
                "søknad1",
                "12342",
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
            } returns knyttJounalPostTilNySakResponse
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
                message["behandlingId"].asUUID() shouldBe behandlingId
                message["oppgaveIder"].map { it.asText() } shouldBe listOf("søknad1", "ettersending1", "ettersending2")
                message["ident"].asText() shouldBe testPersonIdent
            }
        }
        coVerify(exactly = 1) {
            journalpostDokarkiv.knyttJounalPostTilNySak("1", dagpengerFagsakId.toString(), testPersonIdent)
            journalpostDokarkiv.knyttJounalPostTilNySak("2", dagpengerFagsakId.toString(), testPersonIdent)
            journalpostDokarkiv.knyttJounalPostTilNySak("3", dagpengerFagsakId.toString(), testPersonIdent)
        }

        verify {
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(knyttJounalPostTilNySakResponse.nyJournalpostId, 1, dagpengerFagsakId)
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(knyttJounalPostTilNySakResponse.nyJournalpostId, 2, dagpengerFagsakId)
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(knyttJounalPostTilNySakResponse.nyJournalpostId, 3, dagpengerFagsakId)
        }
    }

    private val vedtakFattetIDagpengerJson =
        """
        {
            "@event_name": "vedtak_fattet_utenfor_arena",
            "behandlingId": "$behandlingId",
            "søknadId": "$søknadId",
            "ident": "$testPersonIdent",
            "sakId": "$dagpengerFagsakId"
        }
        """.trimIndent()
}
