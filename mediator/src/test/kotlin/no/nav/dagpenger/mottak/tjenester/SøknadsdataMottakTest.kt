package no.nav.dagpenger.mottak.tjenester

import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.søknadsdata.OrkestratorSøknadFormat
import no.nav.dagpenger.mottak.meldinger.søknadsdata.Søknadsdata
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

internal class SøknadsdataMottakTest {
    private val rapid = TestRapid()
    private val innsendingMediator = mockk<InnsendingMediator>()
    private val søknadsdataSlot = slot<Søknadsdata>()

    init {
        SøknadsdataMottak(innsendingMediator, rapid)
    }

    @Test
    fun `sender søknadsdata videre til innsendingMediator`() {
        every { innsendingMediator.håndter(capture(søknadsdataSlot)) } just Runs

        rapid.sendTestMessage(søknadsdataHendelse(journalpostId = "123"))

        verify(exactly = 1) { innsendingMediator.håndter(any<Søknadsdata>()) }
        søknadsdataSlot.captured.journalpostId() shouldBe "123"
        søknadsdataSlot.captured.søknad().shouldBeInstanceOf<OrkestratorSøknadFormat>()
        søknadsdataSlot.captured.søknad().søknadId() shouldBe "søknad-123"
    }

    private fun søknadsdataHendelse(journalpostId: String): String =
        //language=JSON
        """
        {
          "@event_name": "behov",
          "@final": true,
          "@id": "${UUID.randomUUID()}",
          "@behov": ["Søknadsdata"],
          "@opprettet": "${LocalDateTime.now()}",
          "journalpostId": "$journalpostId",
          "@løsning": {
            "Søknadsdata": {
              "versjon_navn": "OrkestratorSoknad",
              "søknad_uuid": "søknad-123"
            }
          }
        }
        """.trimIndent()
}
