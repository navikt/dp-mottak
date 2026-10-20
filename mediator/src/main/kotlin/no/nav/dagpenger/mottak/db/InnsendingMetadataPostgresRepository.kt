package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import java.util.UUID
import javax.sql.DataSource

internal class InnsendingMetadataPostgresRepository(
    private val ds: DataSource = PostgresDataSourceBuilder.dataSource,
) : InnsendingMetadataRepository {
    override fun hentArenaOppgaver(
        søknadId: UUID,
        ident: String,
    ): List<ArenaOppgave> =
        sessionOf(ds).use { session ->
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
                        AND       COALESCE(
                                      sokn.data -> 'verdi' ->> 'søknad_uuid',
                                      sokn.data ->> 'søknad_uuid'
                                  ) = :soknad_id
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

    private fun hentDagpengerJournalpostIder(
        søknadId: UUID,
        ident: String,
    ): List<String> =
        sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=SQL
                    statement =
                        """
                        SELECT    COALESCE(sak.ny_journalpost_id, inns.journalpostid) as journalpost_id
                        FROM      innsending_v1 inns
                        JOIN      person_innsending_v1 peri         ON peri.id = inns.id
                        JOIN      person_v1 pers                    ON pers.id = peri.personid
                        JOIN      soknad_v1 sokn                    ON sokn.id = inns.id
                        JOIN      sak_v1 sak ON sak.innsending_id = inns.id 
                        WHERE     pers.ident =  :ident
                        AND       COALESCE(
                                      sokn.data -> 'verdi' ->> 'søknad_uuid',
                                      sokn.data ->> 'søknad_uuid'
                                  ) = :soknad_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                            "soknad_id" to søknadId.toString(),
                        ),
                ).map { row ->
                    row.long(columnLabel = "journalpost_id").toString()
                }.asList,
            )
        }

    override fun hentJournalpostIder(
        søknadId: UUID,
        ident: String,
    ): List<String> {
        val dagpengerJournalpostIder = hentDagpengerJournalpostIder(søknadId = søknadId, ident = ident)
        if (dagpengerJournalpostIder.isEmpty()) {
            return hentArenaOppgaver(søknadId = søknadId, ident = ident).map { it.journalpostId }
        }
        return dagpengerJournalpostIder
    }

    override fun opprettKoblingTilNyJournalpostForSak(
        nyJournalpostId: Int,
        innsendingId: Int,
        sakId: UUID,
    ) {
        sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                        INSERT INTO sak_v1(innsending_id, sak_id, ny_journalpost_id)
                        VALUES(:innsending_id, :sak_id, :ny_journalpost_id)
                        ON CONFLICT (innsending_id) DO UPDATE SET sak_id = :sak_id, ny_journalpost_id = :ny_journalpost_id
                        """,
                    mapOf(
                        "innsending_id" to innsendingId,
                        "sak_id" to sakId,
                        "ny_journalpost_id" to nyJournalpostId,
                    ),
                ).asUpdate,
            )
        }
    }

    override fun finnJournalpostIderForSøknad(
        ident: String,
        søknadId: UUID,
    ): List<String> =
        sessionOf(ds).use { session ->
            session.run(
                queryOf(
                    //language=SQL
                    statement =
                        """
                        SELECT    inns.journalpostid AS journalpost_id
                        FROM      innsending_v1 inns
                        JOIN      person_innsending_v1 peri ON inns.id = peri.id
                        JOIN      person_v1 pers            ON pers.id = peri.personid
                        JOIN      soknad_v1 sokn            ON inns.id = sokn.id
                        WHERE     pers.ident =  :ident
                        AND       COALESCE(
                                      sokn.data -> 'verdi' ->> 'søknad_uuid',
                                      sokn.data ->> 'søknad_uuid'
                                  ) = :soknad_id
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                            "soknad_id" to søknadId.toString(),
                        ),
                ).map { row ->
                    row.long(columnLabel = "journalpost_id").toString()
                }.asList,
            )
        }
}
