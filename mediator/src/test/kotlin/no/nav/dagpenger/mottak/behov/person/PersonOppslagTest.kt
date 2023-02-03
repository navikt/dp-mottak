package no.nav.dagpenger.mottak.behov.person

import io.mockk.coEvery
import io.mockk.mockkClass
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import kotlin.test.assertEquals

class PersonOppslagTest {
    private val id = "id"
    private val id2 = "id2"
    private val pdlMock = mockkClass(type = PdlPersondataOppslag::class).also {
        coEvery { it.hentPerson(any()) } returns Pdl.Person(
            navn = "navn",
            aktørId = "aktørId",
            fødselsnummer = "fnr",
            norskTilknytning = false,
            diskresjonskode = "kode"
        )
    }
    private val skjermingMock = mockkClass(type = SkjermingOppslag::class).also {
        coEvery { it.egenAnsatt(id) } returns Result.success(true)
        coEvery { it.egenAnsatt(id2) } returns Result.failure(Throwable("test"))
    }

    @Test
    fun `skal hente person info fra forskjellige kilder`() {
        createPersonOppslag(pdl = pdlMock, skjerming = skjermingMock).let { personOppslag ->
            runBlocking {
                personOppslag.hentPerson(id).let { person ->
                    requireNotNull(person)
                    assertEquals("navn", person.navn)
                    assertEquals("aktørId", person.aktørId)
                    assertEquals("fnr", person.fødselsnummer)
                    assertEquals(false, person.norskTilknytning)
                    assertEquals("kode", person.diskresjonskode)
                    assertEquals(true, person.egenAnsatt)
                }
            }
        }
    }

    @Test
    fun `Håndterer at kall mot skjermings oppslag feiler`() {
        createPersonOppslag(pdl = pdlMock, skjerming = skjermingMock).let { personOppslag ->
            runBlocking {
                personOppslag.hentPerson(id2).let { person ->
                    requireNotNull(person)
                    assertEquals(null, person.egenAnsatt)
                }
            }
        }
    }
}
