package no.nav.dagpenger.mottak.db

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import java.util.UUID
import javax.sql.DataSource

internal class InnsendingMetadataPostgresRepository(private val ds: DataSource = PostgresDataSourceBuilder.dataSource) : InnsendingMetadataRepository {
    override fun hentOppgaverIder(
        søknadId: UUID,
        ident: String,
    ): List<String> {
        return using(sessionOf(ds)) { session ->
            session.run(
                queryOf(
                    //language=SQL
                    statement =
                        """
                        SELECT    aren.oppgaveid 
                        FROM      innsending_v1 inns
                        JOIN      person_innsending_v1 peri ON inns.id = peri.id
                        JOIN      person_v1 pers ON pers.id = peri.persONid
                        JOIN      soknad_v1 sokn ON inns.id = sokn.id
                        JOIN      arenasak_v1 aren ON inns.id = aren.id
                        WHERE     pers.ident =  :ident
                        AND       sokn.data ->> '@id' = ':søknadId'
                        """.trimIndent(),
                    paramMap =
                        mapOf(
                            "ident" to ident,
                            "søknadId" to søknadId.toString(),
                        ),
                ).map { row ->
                    row.string("oppgaveid")
                }.asList,
            )
        }
    }
}
