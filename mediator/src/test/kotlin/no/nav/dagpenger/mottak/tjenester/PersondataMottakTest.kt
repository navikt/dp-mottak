package no.nav.dagpenger.mottak.tjenester

import no.finn.unleash.FakeUnleash
import no.nav.dagpenger.mottak.InnsendingMediator
import no.nav.dagpenger.mottak.db.InMemoryInnsendingRepository
import no.nav.dagpenger.mottak.e2e.TestObservatør
import no.nav.helse.rapids_rivers.testsupport.TestRapid
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.util.UUID

class PersondataMottakTest {
    val testRapid = TestRapid()
    private val innsendingRepository = InMemoryInnsendingRepository()
    private val observatør = TestObservatør()
    private val innsendingMediator = InnsendingMediator(
        innsendingRepository = innsendingRepository,
        rapidsConnection = testRapid,
        unleash = FakeUnleash().also {
            it.enableAll()
        },
        observatører = listOf(observatør)
    )
    init {
        PersondataMottak(innsendingMediator, testRapid)
    }

    @Test
    fun `håndterer svar med null`() {
        testRapid.sendTestMessage(persondataMottattHendelse())
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
