package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaOppgaveTjeneste
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.SlettArenaOppgaveParametere
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import org.junit.jupiter.api.Test
import java.util.UUID

class
VedtakFattetMottakTest {
    private val søknadId = UUID.randomUUID()
    private val fagsakId = "12342"
    private val testPersonIdent = "12345678901"
    private val testOppgaver = listOf("1", "2", "3")

    private val testRapid = TestRapid()
    private val arenaOppgaveTjeneste = mockk<ArenaOppgaveTjeneste>(relaxed = true)
    private val innsendingMetadataRepository =
        mockk<InnsendingMetadataRepository>().also {
            every {
                it.hentOppgaverIder(
                    søknadId = søknadId,
                    ident = testPersonIdent,
                )
            } returns testOppgaver
        }

    init {
        VedtakFattetMottak(
            rapidsConnection = testRapid,
            arenaOppgaveTjeneste = arenaOppgaveTjeneste,
            innsendingMetadataRepository = innsendingMetadataRepository,
        )
    }

    @Test
    fun `Skal slette Arena-oppgaver når vedtak er fattet i fagsystem Dagpenger`() {
        testRapid.sendTestMessage(vedtakFattetIDagpengerJson)
        coVerify(exactly = 1) {
            arenaOppgaveTjeneste.slettArenaOppgaver(
                slettArenaOppgaveParametere =
                    SlettArenaOppgaveParametere(
                        fagsakId = fagsakId,
                        oppgaveIder = testOppgaver,
                    ),
            )
        }
    }

    @Test
    fun `Skal ikke slette Arena-oppgaver når vedtak er fattet i fagsystem Arena`() {
        testRapid.sendTestMessage(vedtakFattetIArenaJson)
        coVerify(exactly = 0) { arenaOppgaveTjeneste.slettArenaOppgaver(any()) }
    }

    private val vedtakFattetIDagpengerJson =
        """
        {
            "@event_name": "vedtak_fattet",
            "ident": "$testPersonIdent",
            "søknadId": "$søknadId",
            "behandlingId": "123e4567-e89b-12d3-a456-426614174001",
            "fagsakId": "$fagsakId",
            "fagsystem": "Dagpenger",
            "automatisk": true
        }
        """.trimIndent()

    private val vedtakFattetIArenaJson =
        """
        {
            "@event_name": "vedtak_fattet",
            "ident": "$testPersonIdent",
            "søknadId": "$søknadId",
            "behandlingId": "123e4567-e89b-12d3-a456-426614174001",
            "fagsakId": "$fagsakId",
            "fagsystem": "Arena",
            "automatisk": true
        }
        """.trimIndent()
}
