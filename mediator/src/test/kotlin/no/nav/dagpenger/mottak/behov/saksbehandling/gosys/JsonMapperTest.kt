package no.nav.dagpenger.mottak.behov.saksbehandling.gosys

import no.nav.dagpenger.mottak.behov.JsonMapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalDate

internal class JsonMapperTest {
    @Test
    fun `test dato formater i gosys request `() {
        val dato = LocalDate.of(2021, 5, 4)
        val oppgaveRequest =
            GosysOppgaveRequest(
                journalpostId = "124",
                beskrivelse = "test",
                aktivDato = dato,
                fristFerdigstillelse = dato,
                tildeltEnhetsnr = "aba",
                aktoerId = "12345500",
            )

        val json = JsonMapper.jacksonJsonAdapter.writeValueAsString(oppgaveRequest)
        val res = JsonMapper.jacksonJsonAdapter.readTree(json)
        assertEquals("2021-05-04", res["aktivDato"].asText())
        assertEquals("2021-05-04", res["fristFerdigstillelse"].asText())
    }
}
