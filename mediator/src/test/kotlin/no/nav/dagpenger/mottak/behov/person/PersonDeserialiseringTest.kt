package no.nav.dagpenger.mottak.behov.person

import no.nav.dagpenger.mottak.behov.person.Pdl.PersonDeserializer.aktørId
import no.nav.dagpenger.mottak.behov.person.Pdl.PersonDeserializer.diskresjonsKode
import no.nav.dagpenger.mottak.behov.person.Pdl.PersonDeserializer.fødselsnummer
import no.nav.dagpenger.mottak.behov.person.Pdl.PersonDeserializer.norskTilknyting
import no.nav.dagpenger.mottak.behov.person.Pdl.PersonDeserializer.personNavn
import no.nav.dagpenger.mottak.behov.GraphqlQuery.Companion.jacksonJsonAdapter
import org.intellij.lang.annotations.Language
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

internal class PersonDeserialiseringTest {
    @Test
    fun `riktig navn`() {
        assertEquals(
            "LITEN hubba BRANNHYDRANT",
            jacksonJsonAdapter.readTree(
                """{"data" :{"navn": [ { "fornavn": "LITEN", "mellomnavn": "hubba",  "etternavn": "BRANNHYDRANT" } ] }} """.trimIndent()
            ).personNavn()
        )
    }

    @Test
    fun `Takler manglende mellom navn`() {
        assertEquals(
            "LITEN BRANNHYDRANT",
            jacksonJsonAdapter.readTree(
                """{ "data": {"navn": [ { "fornavn": "LITEN", "etternavn": "BRANNHYDRANT" } ] } }""".trimIndent()
            ).personNavn()
        )

        assertEquals(
            "LITEN BRANNHYDRANT",
            jacksonJsonAdapter.readTree(
                """{ "data": {"navn": [ { "fornavn": "LITEN", "mellomnavn": null,  "etternavn": "BRANNHYDRANT" } ] } }""".trimIndent()
            ).personNavn()
        )
    }

    @Test
    fun `riktig identer`() {
        @Language("JSON") val json = jacksonJsonAdapter.readTree(
            """{ "data": {"identer": [ { "ident": "13086824072", "gruppe": "FOLKEREGISTERIDENT" }, { "ident": "2797593735308", "gruppe": "AKTORID" } ] }} """.trimIndent()
        )

        assertEquals("2797593735308", json.aktørId())
        assertEquals("13086824072", json.fødselsnummer())
    }

    @Test
    fun `riktig norsk tilknytning`() {
        //language=JSON
        val jsonTrue =
            jacksonJsonAdapter.readTree("""{ "data": { "hentGeografiskTilknytning": { "gtLand": null } } } """.trimIndent())
        assertTrue(jsonTrue.norskTilknyting())

        val jsonFalse =
            jacksonJsonAdapter.readTree("""{ "data": { "hentGeografiskTilknytning": { "gtLand": "sdfsafd" } } } """.trimIndent())
        assertFalse(jsonFalse.norskTilknyting())
    }

    @Test
    fun `riktig diskresjonskode`() {
        //language=JSON
        val strengtFortroligJson =
            jacksonJsonAdapter.readTree("""{ "data": { "hentPerson": { "adressebeskyttelse": [ { "gradering": "STRENGT_FORTROLIG_UTLAND" } ] } } } """.trimIndent())
        assertEquals("STRENGT_FORTROLIG_UTLAND", strengtFortroligJson.diskresjonsKode())

        //language=JSON
        val ukjentGraderingJsone =
            jacksonJsonAdapter.readTree("""{ "data": { "hentPerson": { "adressebeskyttelse": [ { "gradering": null } ] } } } """.trimIndent())
        assertNull(ukjentGraderingJsone.diskresjonsKode())

        @Language("JSON") val ingenBeskyttelseJson =
            jacksonJsonAdapter.readTree("""{ "data": { "hentPerson": { "adressebeskyttelse":[] } } } """.trimIndent())
        assertNull(ingenBeskyttelseJson.diskresjonsKode())
    }
}
