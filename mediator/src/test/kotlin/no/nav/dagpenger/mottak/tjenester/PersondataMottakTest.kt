package no.nav.dagpenger.mottak.tjenester

import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.Before
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PersondataMottakTest {
    private val testRapid = TestRapid()
    private val innsendingMediator = mockk<InnsendingMediator>().also {
        every { it.håndter(any<PersonInformasjonIkkeFunnet>()) } just Runs
    }

    init {
        PersondataMottak(innsendingMediator, testRapid)
    }

    @Before
    fun setup() {
        testRapid.reset()
    }

    @Test
    fun `håndterer svar med null`() {
        testRapid.sendTestMessage(persondataMottattHendelse())
        verify(exactly = 1) {
            innsendingMediator.håndter(any<PersonInformasjonIkkeFunnet>())
        }
    }

    //language=JSON
    private fun persondataMottattHendelse(): String =
        """{
          "@event_name": "behov",
          "@id": "${UUID.randomUUID()}",
          "@behov": [
            "Persondata"
          ],
          "@opprettet" : "${LocalDateTime.now()}",
          "journalpostId": "45645646",
          "@løsning": {
            "Persondata": null
          }
        }
        """.trimIndent()
}
