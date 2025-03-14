package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
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
            innsendingMetadataRepository = innsendingMetadataRepository,
        )
    }

    @Test
    fun `Skal sende ut behov for sletting av Arena-oppgaver når vedtak er fattet i fagsystem Dagpenger`() {
        testRapid.sendTestMessage(vedtakFattetIDagpengerJson)
        testRapid.inspektør.size shouldBe 1
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
