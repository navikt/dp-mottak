package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.coVerify
import io.mockk.mockk
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaOppgaveTjeneste
import org.junit.jupiter.api.Test

class VedtakFattetMottakTest {
    private val testRapid = TestRapid()
    private val arenaOppgaveTjeneste = mockk<ArenaOppgaveTjeneste>(relaxed = true)

    init {
        VedtakFattetMottak(testRapid, arenaOppgaveTjeneste)
    }

    @Test
    fun `Skal slette Arena-oppgaver når vedtak er fattet i fagsystem "Dagpenger"`() {
        testRapid.sendTestMessage(vedtakFattetIDagpengerJson)
        coVerify(exactly = 1) { arenaOppgaveTjeneste.slettArenaOppgaver() }
    }
}

private val vedtakFattetIDagpengerJson =
    """
    {
        "@event_name": "vedtak_fattet",
        "ident": "12345678901",
        "søknadId": "123e4567-e89b-12d3-a456-426614174000",
        "behandlingId": "123e4567-e89b-12d3-a456-426614174001",
        "fagsakId": "12342",
        "fagsystem": "Dagpenger",
        "automatisk": true
    }
    """.trimIndent()
