package no.nav.dagpenger.mottak.tjenester

import io.mockk.CapturingSlot
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.PersonTestData.GENERERT_FØDSELSNUMMER
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.PersonInformasjonIkkeFunnet
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.Before
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PersondataMottakTest {
    private val testRapid = TestRapid()
    private val slot = CapturingSlot<PersonInformasjon>()

    private val innsendingMediator = mockk<InnsendingMediator>().also {
        every { it.håndter(any<PersonInformasjonIkkeFunnet>()) } just Runs
        every { it.håndter(capture(slot)) } just Runs
    }

    init {
        PersondataMottak(innsendingMediator, testRapid)
    }

    @Before
    fun setup() {
        testRapid.reset()
        slot.clear()
    }

    @Test
    fun `håndterer svar med null`() {
        testRapid.sendTestMessage(nullPersonData)
        verify(exactly = 1) {
            innsendingMediator.håndter(any<PersonInformasjonIkkeFunnet>())
        }
    }

    @Test
    fun `parser melding riktig`() {
        testRapid.sendTestMessage(personDataHendelse)
        slot.captured.person().let {
            assertEquals(it.navn, "navn")
            assertEquals(it.aktørId, "aktorId")
            assertEquals(it.ident, GENERERT_FØDSELSNUMMER)
            assertEquals(it.norskTilknytning, true)
            assertEquals(it.diskresjonskode, false)
            assertEquals(it.egenAnsatt, true)
        }
    }

    //language=JSON
    private val personDataHendelse: String = """{
      "@event_name": "behov",
      "@id": "${UUID.randomUUID()}",
      "@behov": [
        "Persondata"
      ],
      "@opprettet" : "${LocalDateTime.now()}",
      "journalpostId": "45645646",
      "@løsning": {
        "Persondata": {
         "navn": "navn",
         "aktørId": "aktorId",
         "fødselsnummer": "$GENERERT_FØDSELSNUMMER",
         "norskTilknytning": true,
         "diskresjonskode": "kode",
         "egenAnsatt": true
        }
      }
    }
    """.trimIndent()

    //language=JSON
    private val nullPersonData: String = """{
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
