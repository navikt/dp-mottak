package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.Runs
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.just
import io.mockk.mockk
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaKlient
import org.junit.jupiter.api.Test
import java.util.UUID

class SlettArenaOppgaverBehovløserTest {
    private val testRapid = TestRapid()
    private val behandlingId = UUID.randomUUID()
    private val arenaFagsakId = "123"
    private val ident = "12345612345"

    @Test
    fun `Skal behandle pakker med alle required keys`() {
        val arenaKlient =
            mockk<ArenaKlient>().also {
                coEvery { it.slettOppgaver(any(), any()) } just Runs
            }

        SlettArenaOppgaverBehovløser(testRapid, arenaKlient)
        testRapid.sendTestMessage(slettArenaOppgaverBehov)

        coVerify(exactly = 1) {
            arenaKlient.slettOppgaver(arenaFagsakId, listOf("1", "2", "3"))
        }
    }

    //language=JSON
    private val slettArenaOppgaverBehov =
        """
        {
          "@event_name" : "behov",
          "@behovId" : "def41e2f-fb8f-4b38-9abd-2a666488f854",
          "@behov" : [ "slett_arena_oppgaver" ],
          "behandlingId" : "$behandlingId",
          "arenaFagsakId" : "$arenaFagsakId",
          "ident" : "$ident",
          "oppgaveIder" : [ "1", "2", "3" ],
          "@id" : "841bc42e-2989-4341-89a9-947ea58dbd14",
          "@opprettet" : "2024-10-18T09:21:10.277026"
        }
        """.trimIndent()
}
