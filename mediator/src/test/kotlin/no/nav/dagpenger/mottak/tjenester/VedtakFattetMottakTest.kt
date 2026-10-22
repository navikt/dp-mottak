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
    private val søknadIdForPersonIDpSak = UUID.randomUUID()
    private val behandlingId = UUID.randomUUID()
    private val dagpengerFagsakId = UUID.randomUUID()
    private val testPersonIdent = "12345678901"
    private val knyttJounalPostTilNySakResponse = KnyttJounalPostTilNySakResponse("12")
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
                it.hentArenaOppgaver(
                    søknadId = søknadIdForPersonIDpSak,
                    ident = testPersonIdent,
                )
            } returns emptyList()

            every {
                it.opprettKoblingTilNyJournalpostForSak(
                    nyJournalpostId = any(),
                    innsendingId = any(),
                    sakId = any(),
                )
            } just Runs
            every {
                it.finnJournalpostIderForSøknad(
                    ident = testPersonIdent,
                    søknadId = søknadIdForPersonIDpSak,
                )
            } returns listOf(Pair("1", 1), Pair("2", 2), Pair("3", 3))
        }

    private val journalpostDokarkiv =
        mockk<JournalpostDokarkiv>().also {
            coEvery {
                it.knyttJounalPostTilNySak(any(), any(), any())
            } returns knyttJounalPostTilNySakResponse
            coEvery {
                it.feilregistrerSakstilknytning(any())
            } just Runs
        }

    init {
        VedtakFattetMottak(
            rapidsConnection = testRapid,
            innsendingMetadataRepository = innsendingMetadataRepository,
            journalpostDokarkiv = journalpostDokarkiv,
        )
    }

    @Test
    fun `Skal ikke sende ut behov om sletting av Arena oppgaver eller oppdatere fagsak i Joark når vedtak_fattet_utenfor_arena mottas og oppgave ikke finnes i Arena`() {
        testRapid.sendTestMessage(vedtakFattetIDagpengerJson(søknadId = søknadIdForPersonIDpSak, eventName = "vedtak_fattet_utenfor_arena"))

        testRapid.inspektør.size shouldBe 0
        coVerify(exactly = 0) {
            journalpostDokarkiv.knyttJounalPostTilNySak(any(), any(), any())
        }
        verify(exactly = 0) {
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(any(), any(), any())
        }
    }

    @Test
    fun `Skal knytte journalpost til DP-sak og sende ut behov for sletting av Arena-oppgaver når vedtak_fattet_utenfor_arena mottas og det er opprettet oppgave i Arena`() {
        testRapid.sendTestMessage(vedtakFattetIDagpengerJson(søknadId = søknadId, eventName = "vedtak_fattet_utenfor_arena"))
        testRapid.inspektør.let { inspektør ->
            inspektør.size shouldBe 1
            inspektør.key(0) shouldBe testPersonIdent
            inspektør.message(0).let { message ->
                message["@event_name"].asString() shouldBe "behov"
                message["@behov"].values().map { it.asString() }.single() shouldBe "slett_arena_oppgaver"
                message["behandlingId"].asUUID() shouldBe behandlingId
                message["oppgaveIder"].values().map { it.asString() } shouldBe listOf("søknad1", "ettersending1", "ettersending2")
                message["ident"].asString() shouldBe testPersonIdent
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

    @Test
    fun `Skal knytte journalpost til ny DP-sak og feilregistrere gammel journalpost når vedtak_fattet_i_ny_sak mottas`() {
        testRapid.sendTestMessage(vedtakFattetIDagpengerJson(søknadId = søknadIdForPersonIDpSak, eventName = "vedtak_fattet_i_ny_sak"))
        testRapid.inspektør.let { inspektør ->
            inspektør.size shouldBe 0
        }
        coVerify(exactly = 1) {
            journalpostDokarkiv.knyttJounalPostTilNySak("1", dagpengerFagsakId.toString(), testPersonIdent)
            journalpostDokarkiv.knyttJounalPostTilNySak("2", dagpengerFagsakId.toString(), testPersonIdent)
            journalpostDokarkiv.knyttJounalPostTilNySak("3", dagpengerFagsakId.toString(), testPersonIdent)
            journalpostDokarkiv.feilregistrerSakstilknytning("1")
            journalpostDokarkiv.feilregistrerSakstilknytning("2")
            journalpostDokarkiv.feilregistrerSakstilknytning("3")
        }

        verify {
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(knyttJounalPostTilNySakResponse.nyJournalpostId, 1, dagpengerFagsakId)
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(knyttJounalPostTilNySakResponse.nyJournalpostId, 2, dagpengerFagsakId)
            innsendingMetadataRepository.opprettKoblingTilNyJournalpostForSak(knyttJounalPostTilNySakResponse.nyJournalpostId, 3, dagpengerFagsakId)
        }
    }

    private fun vedtakFattetIDagpengerJson(
        søknadId: UUID,
        eventName: String,
    ) = """
        {
            "@event_name": "$eventName",
            "behandlingId": "$behandlingId",
            "søknadId": "$søknadId",
            "ident": "$testPersonIdent",
            "sakId": "$dagpengerFagsakId"
        }
        """.trimIndent()
}
