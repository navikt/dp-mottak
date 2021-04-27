package no.nav.dagpenger.mottak.db

import kotliquery.Query
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingVisitor
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import org.postgresql.util.PGobject
import java.time.ZonedDateTime
import javax.sql.DataSource

internal class InnsendingPostgresRepository(private val datasource: DataSource = Config.dataSource) :
    InnsendingRepository {
    override fun hent(journalpostId: String): Innsending =
        using(sessionOf(datasource)) { session ->
            session.run(
                queryOf( //language=PostgreSQL
                    "SELECT * FROM innsending_v1  WHERE journalpostId = :id",
                    mapOf("id" to journalpostId.toLong())
                ).map { Innsending(it.long("journalpostId").toString()) }.asSingle
            )?.let {
                it
            }
        } ?: throw IllegalArgumentException("Kunne ikke finnne innsending med id $journalpostId")

    override fun lagre(innsending: Innsending): Int {
        val visitor = PersistenceVisitor(innsending)
        return using(sessionOf(datasource)) { session ->
            session.transaction { tx -> visitor.lagreQueries.map { tx.run(it.asUpdate) }.sum() }
        }
    }

    private class PersistenceVisitor(val innsending: Innsending) : InnsendingVisitor {

        val lagreQueries: MutableList<Query> = mutableListOf()
        private val jpId = innsending.journalpostId().toLong()

        init {
            innsending.accept(this)
        }

        override fun visitTilstand(tilstandType: Innsending.Tilstand) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    "INSERT INTO innsending_v1(journalpostId,tilstand) VALUES(:jpId, :tilstand)",
                    mapOf(
                        "jpId" to jpId,
                        "tilstand" to tilstandType.type.name
                    )
                )
            )
        }

        override fun visitInnsending(oppfyllerMinsteArbeidsinntekt: Boolean?, eksisterendeSaker: Boolean?) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    "INSERT INTO innsending_oppfyller_minsteinntekt_v1(journalpostId,verdi) VALUES (:jpId, :verdi)",
                    mapOf(
                        "jpId" to jpId,
                        "verdi" to oppfyllerMinsteArbeidsinntekt
                    )
                )
            )
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    "INSERT INTO innsending_eksisterende_arena_saker_v1(journalpostId,verdi) VALUES (:jpId, :verdi)",
                    mapOf(
                        "jpId" to jpId,
                        "verdi" to eksisterendeSaker
                    )
                )
            )
        }

        override fun visitJournalpost(
            journalpostId: String,
            journalpostStatus: String,
            bruker: Journalpost.Bruker?,
            behandlingstema: String?,
            registrertDato: ZonedDateTime,
            dokumenter: List<Journalpost.DokumentInfo>
        ) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    "INSERT INTO journalpost_v1(journalpostId,brukerId,brukerType,behandlingstema,registrertDato) VALUES(:jpId, :brukerId, :brukerType, :behandlingstema,:registrertDato)",
                    mapOf(
                        "jpId" to jpId,
                        "brukerId" to bruker?.id,
                        "brukerType" to bruker?.type?.name,
                        "behandlingstema" to behandlingstema,
                        "registrertDato" to registrertDato
                    )
                )
            )
            val dokumentliste =
                dokumenter.joinToString { """('$jpId','${it.tittel}','${it.dokumentInfoId}','${it.brevkode}')""" }
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    "INSERT INTO journalpost_dokumenter_v1(journalpostId,tittel,dokumentInfoId,brevkode) VALUES $dokumentliste"
                )
            )
        }

        override fun visitSøknad(søknad: Søknadsdata.Søknad?) {
            søknad?.let {
                lagreQueries.add(
                    queryOf( //language=PostgreSQL
                        "INSERT INTO soknad_v1(journalpostId,data) VALUES(:jpId,:data)",
                        mapOf(
                            "jpId" to jpId,
                            "data" to PGobject().apply {
                                type = "jsonb"
                                value = it.data.toString()
                            }
                        )
                    )
                )
            }
        }

        override fun visitPerson(
            aktørId: String,
            fødselsnummer: String,
            norskTilknytning: Boolean,
            diskresjonskode: Boolean
        ) {

            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    """WITH inserted_id as 
                        (INSERT INTO person_v1(fødselsnummer,aktørId) VALUES(:fnr,:aktoerId) RETURNING id)
                        INSERT INTO person_innsending_v1(journalpostId,personId,norsktilknytning,diskresjonskode) 
                        SELECT :jpId, id, :norskTilknytning, :diskresjonskode  from inserted_id""".trimMargin(),
                    mapOf(
                        "jpId" to jpId,
                        "fnr" to fødselsnummer,
                        "aktoerId" to aktørId,
                        "norskTilknytning" to norskTilknytning,
                        "diskresjonskode" to diskresjonskode
                    )
                )
            )
        }

        override fun visitArenaSak(oppgaveId: String, fagsakId: String) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    """
                        INSERT INTO arenasak_v1(fagsakId, oppgaveId, journalpostId) VALUES(:fagsakId,:opgaveId,:journalpostId) 
                        """,
                    mapOf(
                        "journalpostId" to jpId,
                        "fagsakId" to fagsakId,
                        "oppgaveId" to oppgaveId
                    )
                )
            )
        }
    }

}
