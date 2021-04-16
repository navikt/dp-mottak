package no.nav.dagpenger.mottak.behov.vilkårtester

import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertDoesNotThrow
import java.time.LocalDate

class MinsteinntektSerialiseringTest {
    private val regelkontekst = RegelKontekst("567890")

    @Test
    fun `skal serialisere BehovRequest riktig`() {
        assertDoesNotThrow { BehovRequest("11111", regelkontekst, LocalDate.now().toString()).toJson() }
    }
}
