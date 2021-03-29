package no.nav.dagpener.mottak

import no.nav.dagpenger.mottak.Behovtype
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.meldinger.JoarkHendelse
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import java.util.UUID

class InnsendingTest {

    @Test
    fun `skal håndtere joark hendelse`() {
        val journalpostId = "12345"
        val innsending = Innsending(
            id = UUID.randomUUID(),
            journalpostId = journalpostId
        )
        val joarkHendelse = JoarkHendelse(
            journalpostId = journalpostId,
            hendelseType = "MIDLERTIDIG",
            journalpostStatus = "MOTTATT"
        )

        innsending.håndter(joarkHendelse)

        assertFalse(joarkHendelse.behov().isEmpty())
        val behov = joarkHendelse.behov().first()
        assertEquals(Behovtype.Journalpost, behov.type)
    }
}
