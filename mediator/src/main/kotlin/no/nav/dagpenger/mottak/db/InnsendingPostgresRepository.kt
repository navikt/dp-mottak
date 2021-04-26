package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.meldinger.ArenaOppgaveOpprettet
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.PersonInformasjon
import no.nav.dagpenger.mottak.meldinger.Søknadsdata.Søknad
import kotlin.reflect.full.primaryConstructor
import kotlin.reflect.jvm.isAccessible

internal class InnsendingPostgresRepository : InnsendingRepository {
    override fun hent(journalpostId: String): Innsending {
        return using(sessionOf(Config.dataSource)) { session ->
            session.run(
                queryOf( //language=PostgreSQL
                    "SELECT * FROM innsending_v1 WHERE journalpostId = :id",
                    mapOf("id" to journalpostId)
                ).map { Innsending(it.string("journalpostId")) }.asSingle
            )?.let {
                it
            }
        } ?: throw IllegalArgumentException("hubba")
    }

    override fun lagre(innsending: Innsending): Boolean {
        TODO("Not yet implemented")
    }
}

internal fun createInnsending(
    journalpostId: String,
    tilstand: String,
    journalpost: Journalpost?,
    søknad: Søknad?,
    oppfyllerMinsteArbeidsinntekt: Boolean?,
    eksisterendeSaker: Boolean?,
    person: PersonInformasjon.Person?,
    arenaSak: ArenaOppgaveOpprettet.ArenaSak?,
    aktivitetslogg: Aktivitetslogg
): Innsending {

    return Innsending::class.primaryConstructor!!
        .apply { isAccessible = true }
        .call(journalpostId, tilstand)
}
