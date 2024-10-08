package no.nav.dagpenger.mottak

import com.fasterxml.jackson.databind.JsonNode
import com.github.navikt.tbd_libs.rapids_and_rivers.test_support.TestRapid
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertDoesNotThrow
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.LocalDateTime
import java.util.UUID

internal class BehovMediatorTest {
    private companion object {
        private const val JOURNALPOST_ID = "journalpostId"
        private lateinit var behovMediator: BehovMediator
    }

    private val testRapid = TestRapid()
    private lateinit var aktivitetslogg: Aktivitetslogg
    private lateinit var innsending: Innsending

    @BeforeEach
    fun setup() {
        innsending = Innsending(journalpostId = JOURNALPOST_ID)
        aktivitetslogg = Aktivitetslogg()
        behovMediator =
            BehovMediator(
                rapidsConnection = testRapid,
                sikkerLogg = mockk(relaxed = true),
            )
        testRapid.reset()
    }

    @Test
    internal fun `grupperer behov`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(innsending)

        hendelse.behov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata,
            "Trenger personopplysninger",
            mapOf(
                "aktørId" to "12344",
            ),
        )
        hendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Søknadsdata, "Trenger Søknadsdata")
        hendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.OpprettStartVedtakOppgave, "Trenger OpprettStartVedtakOppgave")

        behovMediator.håndter(hendelse)

        val inspektør = testRapid.inspektør

        assertEquals(1, inspektør.size)
        assertEquals(JOURNALPOST_ID, inspektør.key(0))

        inspektør.message(0).also {
            assertEquals("behov", it["@event_name"].asText())
            assertTrue(it.hasNonNull("@id"))
            assertDoesNotThrow { UUID.fromString(it["@id"].asText()) }
            assertTrue(it.hasNonNull("@opprettet"))
            assertDoesNotThrow { LocalDateTime.parse(it["@opprettet"].asText()) }
            assertEquals(listOf("Persondata", "Søknadsdata", "OpprettStartVedtakOppgave"), it["@behov"].map(JsonNode::asText))
            assertEquals("behov", it["@event_name"].asText())
            assertEquals("12344", it["aktørId"].asText())
            assertEquals(JOURNALPOST_ID, it["journalpostId"].asText())
        }
    }

    @Test
    fun `Legger på @final på pakker som bare har ett behov`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(innsending)

        hendelse.behov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata,
            "Trenger personopplysninger",
            mapOf(
                "aktørId" to "12344",
            ),
        )
        behovMediator.håndter(hendelse)
        testRapid.inspektør.message(0).also {
            assertEquals(true, it["@final"].asBoolean())
        }
    }

    @Test
    internal fun `sjekker etter duplikatverdier`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(innsending)
        hendelse.behov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata,
            "Trenger personopplysninger",
            mapOf(
                "aktørId" to "12344",
            ),
        )
        hendelse.behov(
            Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata,
            "Trenger personopplysninger",
            mapOf(
                "aktørId" to "12344",
            ),
        )

        assertThrows<IllegalArgumentException> { behovMediator.håndter(hendelse) }
    }

    @Test
    internal fun `kan ikke produsere samme behov`() {
        val hendelse = TestHendelse("Hendelse1", aktivitetslogg.barn())
        hendelse.kontekst(innsending)
        hendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata, "Trenger Persondata")
        hendelse.behov(Aktivitetslogg.Aktivitet.Behov.Behovtype.Persondata, "Trenger Persondata")

        assertThrows<IllegalArgumentException> { behovMediator.håndter(hendelse) }
    }

    private class TestHendelse(
        private val melding: String,
        internal val logg: Aktivitetslogg,
    ) : Hendelse(logg),
        Aktivitetskontekst {
        init {
            logg.kontekst(this)
        }

        override fun journalpostId(): String = JOURNALPOST_ID

        override fun toSpesifikkKontekst() = SpesifikkKontekst("TestHendelse")

        override fun kontekst(kontekst: Aktivitetskontekst) {
            logg.kontekst(kontekst)
        }
    }
}
