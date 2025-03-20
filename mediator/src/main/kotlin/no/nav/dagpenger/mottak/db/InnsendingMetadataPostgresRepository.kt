package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.UUID
import javax.sql.DataSource

internal class InnsendingMetadataPostgresRepository(private val ds: DataSource = PostgresDataSourceBuilder.dataSource) : InnsendingMetadataRepository {
    override fun hentArenaOppgaver(
        søknadId: UUID,
        ident: String,
    ): List<ArenaOppgave> {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=SQL
                    statement =
                        """
                        SELECT    aren.oppgaveid , aren.fagsakid, inns.journalpostid, inns.id as innsending_id 
                        FROM      innsending_v1 inns
                        JOIN      person_innsending_v1 peri ON inns.id = peri.id
                        JOIN      person_v1 pers            ON pers.id = peri.personid
                        JOIN      soknad_v1 sokn            ON inns.id = sokn.id
                        JOIN      arenasak_v1 aren          ON inns.id = aren.id
                        WHERE     pers.ident =  :ident
                        AND       sokn.data ->> '@id' = :soknad_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                            "soknad_id" to søknadId.toString(),
                        ),
                ).map { row ->
                    ArenaOppgave(
                        oppgaveId = row.string(columnLabel = "oppgaveid"),
                        fagsakId = row.stringOrNull(columnLabel = "fagsakid"),
                        journalpostId = row.int(columnLabel = "journalpostid").toString(),
                        innsendingId = row.int(columnLabel = "innsending_id"),
                    )
                }.asList,
            )
        }
    }

    private fun hentDagpengerJournalpostIder(
        søknadId: UUID,
        ident: String,
    ): List<String> {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=SQL
                    statement =
                        """
                        SELECT    jour.journalpost_id 
                        FROM      innsending_v1 inns
                        JOIN      person_innsending_v1 peri         ON peri.id = inns.id
                        JOIN      person_v1 pers                    ON pers.id = peri.personid
                        JOIN      soknad_v1 sokn                    ON sokn.id = inns.id
                        JOIN      journalpost_dagpenger_sak_v1 jour ON jour.innsending_id = inns.id 
                        WHERE     pers.ident =  :ident
                        AND       sokn.data ->> '@id' = :soknad_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                            "soknad_id" to søknadId.toString(),
                        ),
                ).map { row ->
                    row.int(columnLabel = "journalpost_id").toString()
                }.asList,
            )
        }
    }

    override fun hentJournalpostIder(
        søknadId: UUID,
        ident: String,
    ): List<String> {
        val dagpengerJournalpostIder = hentDagpengerJournalpostIder(søknadId = søknadId, ident = ident)
        if (dagpengerJournalpostIder.size == 0) {
            return hentArenaOppgaver(søknadId = søknadId, ident = ident).map { it.journalpostId }
        }
        return dagpengerJournalpostIder
    }

    override fun opprettKoblingTilNyJournalpostForSak(
        jounalpostId: Int,
        innsendingId: Int,
        fagsakId: UUID,
    ) {
        using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                        INSERT INTO journalpost_dagpenger_sak_v1(journalpost_id, fagsak_id, innsending_id)
                        VALUES(:journalpost_id, :fagsak_id, :innsending_id)
                        """,
                    mapOf(
                        "journalpost_id" to jounalpostId,
                        "fagsak_id" to fagsakId,
                        "innsending_id" to innsendingId,
                    ),
                ).asUpdate,
            )
        }
    }
}
