package no.nav.dagpenger.mottak.behov.person

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.withContext
import mu.KotlinLogging

private val logg = KotlinLogging.logger {}
private val sikkerlogg = KotlinLogging.logger("tjenestekall.mottak.personoppslag")

internal interface PersonOppslag {
    suspend fun hentPerson(id: String): Person?
    data class Person(
        val navn: String,
        val aktørId: String,
        val fødselsnummer: String,
        val norskTilknytning: Boolean,
        val diskresjonskode: String?,
        val egenAnsatt: Boolean?,
    )
}

internal fun createPersonOppslag(pdl: PdlPersondataOppslag, skjerming: SkjermingOppslag): PersonOppslag {
    return object : PersonOppslag {
        override suspend fun hentPerson(id: String): PersonOppslag.Person? {
            return withContext(Dispatchers.IO) {
                val pdlPerson = async { pdl.hentPerson(id) }
                val egenAnsatt = async { skjerming.egenAnsatt(id) }.await()
                    .onFailure {
                        logg.error(it) { "Feil ved oppslag mot skjerming(uthenting av egen ansatt info)" }
                        sikkerlogg.error { "Feil ved oppslag mot skjerming for fødsenummer: $id" }
                    }.getOrThrow()

                pdlPerson.await()?.let {
                    PersonOppslag.Person(
                        navn = it.navn,
                        aktørId = it.aktørId,
                        fødselsnummer = it.fødselsnummer,
                        norskTilknytning = it.norskTilknytning,
                        diskresjonskode = it.diskresjonskode,
                        egenAnsatt = egenAnsatt,
                    )
                }
            }
        }
    }
}
