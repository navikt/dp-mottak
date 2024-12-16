package no.nav.dagpenger.mottak.db

import io.ktor.utils.io.core.use
import kotliquery.Query
import kotliquery.Row
import kotliquery.TransactionalSession
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingPeriode
import no.nav.dagpenger.mottak.InnsendingVisitor
import no.nav.dagpenger.mottak.SøknadOppslag
import no.nav.dagpenger.mottak.api.Periode
import no.nav.dagpenger.mottak.behov.JsonMapper
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.serder.InnsendingData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerTypeData
import no.nav.dagpenger.mottak.serder.InnsendingData.TilstandData
import no.nav.dagpenger.mottak.toMap
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import javax.sql.DataSource

internal class InnsendingPostgresRepository(
    private val datasource: DataSource = PostgresDataSourceBuilder.dataSource,
) : InnsendingRepository {
    // language=PostgreSQL
    private val hentDataSql =
        """
        select innsending.id                             as "internId",
               innsending.journalpostid                  as "journalpostId",
               innsending.tilstand                       as "tilstand",
               innsending.opprettet                      as "opprettet",
               journalpost.status                        as "status",
               journalpost.brukerid                      as "brukerId",
               journalpost.brukertype                    as "brukerType",
               journalpost.behandlingstema               as "behandlingstema",
               journalpost.registrertdato                as "registrertdato",
               journalpost.journalforendeEnhet           as "journalforendeEnhet",
               arenasak.fagsakid                         as "fagsakId",
               arenasak.oppgaveId                        as "oppgaveId",
               soknad.data                               as "søknadsData",
               person_innsending.navn                    as "navn",
               person_innsending.diskresjonskode         as "diskresjonsKode",
               person_innsending.norsktilknytning        as "norsktilknytning",
               person_innsending.egenansatt              as "egenansatt",
               person.ident                              as "ident",
               person.aktørid                            as "aktørid",
               aktivitetslogg.data                       as "aktivitetslogg"
        from innsending_v1 as innsending
                 left join journalpost_v1 journalpost on innsending.id = journalpost.id
                 left join aktivitetslogg_v1 aktivitetslogg on innsending.id = aktivitetslogg.id
                 left join arenasak_v1 arenasak on innsending.id = arenasak.id
                 left join soknad_v1 soknad on innsending.id = soknad.id
                 left join person_innsending_v1 person_innsending on innsending.id = person_innsending.id
                 left join person_v1 person on person_innsending.personid = person.id
        where innsending.journalpostId = :jpId;
        """.trimIndent()

    fun Row.booleanOrNull(columnLabel: String) = this.stringOrNull(columnLabel)?.let { it == "t" }

    override fun hent(journalpostId: String): Innsending? =
        using(sessionOf(datasource)) { session ->
            session
                .run(
                    queryOf(
                        hentDataSql,
                        mapOf("jpId" to journalpostId.toLong()),
                    ).map { row ->
                        InnsendingData(
                            id = row.long("internId"),
                            journalpostId = row.long("journalpostId").toString(),
                            tilstand = TilstandData(TilstandData.InnsendingTilstandTypeData.valueOf(row.string("tilstand"))),
                            journalpostData =
                                row.localDateTimeOrNull("registrertDato")?.let {
                                    InnsendingData.JournalpostData(
                                        journalpostId = row.long("journalpostId").toString(),
                                        bruker =
                                            row.stringOrNull("brukerType")?.let { type ->
                                                BrukerData(BrukerTypeData.valueOf(type), row.string("brukerId"))
                                            },
                                        journalpostStatus = row.string("status"),
                                        behandlingstema = row.stringOrNull("behandlingstema"),
                                        journalførendeEnhet = row.stringOrNull("journalforendeEnhet"),
                                        registertDato = it,
                                        dokumenter = listOf(),
                                    )
                                },
                            personData =
                                row.stringOrNull("ident")?.let {
                                    InnsendingData.PersonData(
                                        navn = row.string("navn"),
                                        aktørId = row.string("aktørId"),
                                        fødselsnummer = row.string("ident"),
                                        norskTilknytning = row.boolean("norsktilknytning"),
                                        diskresjonskode = row.boolean("diskresjonskode"),
                                        egenAnsatt = row.boolean("egenansatt"),
                                    )
                                },
                            arenaSakData =
                                row.stringOrNull("fagsakId")?.let {
                                    InnsendingData.ArenaSakData(
                                        oppgaveId = row.string("oppgaveId"),
                                        fagsakId = it,
                                    )
                                },
                            søknadsData =
                                row.binaryStreamOrNull("søknadsData")?.use {
                                    JsonMapper.jacksonJsonAdapter.readTree(it)
                                },
                            aktivitetslogg =
                                row.binaryStream("aktivitetslogg").use {
                                    JsonMapper.jacksonJsonAdapter.readValue(
                                        it,
                                        InnsendingData.AktivitetsloggData::class.java,
                                    )
                                },
                        )
                    }.asSingle,
                )?.let {
                    val dokumenter =
                        session.run(
                            queryOf(
                                //language=PostgreSQL
                                """
                                SELECT 
                                brevkode,
                                tittel,
                                dokumentinfoid,
                                hovedDokument
                                FROM journalpost_dokumenter_v1 WHERE id = :internId
                                """.trimIndent(),
                                mapOf(
                                    "internId" to it.id,
                                ),
                            ).map { row ->
                                InnsendingData.JournalpostData.DokumentInfoData(
                                    brevkode = row.string("brevkode"),
                                    tittel = row.string("tittel"),
                                    dokumentInfoId = row.string("dokumentInfoId"),
                                    hovedDokument = row.boolean("hovedDokument"),
                                )
                            }.asList,
                        )

                    it.copy(journalpostData = it.journalpostData?.copy(dokumenter = dokumenter)).createInnsending()
                }
        }

    override fun lagre(innsending: Innsending): Int {
        return using(sessionOf(datasource)) { session ->
            return@using session.transaction { transactionalSession ->
                val internId =
                    transactionalSession.run(
                        queryOf(
                            //language=PostgreSQL
                            """ 
                            SELECT id from innsending_v1 where journalpostid = :journalpostId
                            """.trimIndent(),
                            mapOf("journalpostId" to innsending.journalpostId().toLong()),
                        ).map {
                            it.longOrNull("id")
                        }.asSingle,
                    ) ?: NyInnsendingQueryVisiotor(innsending, transactionalSession).internId
                val visitor = InnsendingQueryVisitor(innsending, internId)
                visitor.lagreQueries.sumOf { transactionalSession.run(it.asUpdate) }
            }
        }
    }

    override fun forPeriode(periode: Periode) =
        using(sessionOf(datasource)) { session ->
            session.run(
                queryOf(
                    //language=PostgreSQL
                    """
                    SELECT person.ident, journalpost.registrertdato, journalpostid FROM innsending_v1 AS innsending
                    LEFT JOIN person_innsending_v1 person_innsending on innsending.id = person_innsending.id
                    LEFT JOIN person_v1 person on person_innsending.personid = person.id
                    LEFT JOIN journalpost_v1 journalpost on innsending.id = journalpost.id
                    LEFT JOIN journalpost_dokumenter_v1 dokumenter on journalpost.id = dokumenter.id
                    WHERE journalpost.registrertdato BETWEEN :fom::timestamp AND :tom::timestamp 
                    AND dokumenter.brevkode in ('NAV 04-01.03', 'NAV 04-01.04')
                    ORDER BY registrertdato 
                    """.trimIndent(),
                    mapOf(
                        "fom" to periode.fom,
                        "tom" to periode.tom,
                    ),
                ).map { row ->
                    InnsendingPeriode(
                        ident = row.stringOrNull("ident") ?: "Ident mangler",
                        registrertDato = row.localDateTime("registrertDato"),
                        journalpostId = row.string("journalpostId"),
                    )
                }.asList,
            )
        }

    class NyInnsendingQueryVisiotor(
        private val innsending: Innsending,
        private val transactionalSession: TransactionalSession,
    ) : InnsendingVisitor {
        var internId: Long = 0

        init {
            innsending.accept(this)
        }

        override fun visitTilstand(tilstandType: Innsending.Tilstand) {
            internId = transactionalSession.run(
                queryOf(
                    //language=PostgreSQL
                    "INSERT INTO innsending_v1(journalpostId,tilstand) VALUES(:jpId, :tilstand) RETURNING id",
                    mapOf(
                        "jpId" to innsending.journalpostId().toLong(),
                        "tilstand" to tilstandType.type.name,
                    ),
                ).map { row ->
                    row.long("id")
                }.asSingle,
            ) ?: throw IllegalArgumentException("Feil ved opprettelse av Innsending! Noe er galt")
        }
    }

    class InnsendingQueryVisitor(
        innsending: Innsending,
        private val internId: Long,
    ) : InnsendingVisitor {
        val lagreQueries: MutableList<Query> = mutableListOf()

        init {
            innsending.accept(this)
        }

        override fun visitTilstand(tilstandType: Innsending.Tilstand) {
            lagreQueries.add(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO innsending_v1(id,tilstand) VALUES(:id, :tilstand) 
                    ON CONFLICT(id) DO UPDATE  set tilstand = :tilstand
                    """.trimIndent(),
                    mapOf(
                        "id" to internId,
                        "tilstand" to tilstandType.type.name,
                    ),
                ),
            )
        }

        override fun visitJournalpost(
            journalpostId: String,
            journalpostStatus: String,
            bruker: Journalpost.Bruker?,
            behandlingstema: String?,
            journalførendeEnhet: String?,
            registrertDato: LocalDateTime,
            dokumenter: List<Journalpost.DokumentInfo>,
        ) {
            lagreQueries.add(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO journalpost_v1(id,status, brukerId,brukerType,behandlingstema,journalforendeEnhet,registrertDato) 
                    VALUES(:id, :status,:brukerId, :brukerType, :behandlingstema,:journalforendeEnhet,:registrertDato)
                    ON CONFLICT(id) DO NOTHING 
                    """.trimIndent(),
                    mapOf(
                        "id" to internId,
                        "status" to journalpostStatus,
                        "brukerId" to bruker?.id,
                        "brukerType" to bruker?.type?.name,
                        "behandlingstema" to behandlingstema,
                        "journalforendeEnhet" to journalførendeEnhet,
                        "registrertDato" to registrertDato,
                    ),
                ),
            )

            val dokumentQueries =
                dokumenter.map {
                    queryOf(
                        //language=PostgreSQL
                        """
                        INSERT INTO journalpost_dokumenter_v1(id,tittel,dokumentInfoId,brevkode, hovedDokument) 
                        VALUES(:internId, :tittel, :dokumentInfoId, :brevkode, :hovedDokument) ON CONFLICT DO NOTHING
                        """.trimIndent(),
                        mapOf(
                            "internId" to internId,
                            "tittel" to it.tittel,
                            "dokumentInfoId" to it.dokumentInfoId.toLong(),
                            "brevkode" to it.brevkode,
                            "hovedDokument" to it.hovedDokument,
                        ),
                    )
                }

            if (dokumentQueries.isNotEmpty()) {
                lagreQueries.addAll(dokumentQueries)
            }
        }

        override fun visitSøknad(søknad: SøknadOppslag?) {
            søknad?.let {
                lagreQueries.add(
                    queryOf(
                        //language=PostgreSQL
                        "INSERT INTO soknad_v1(id,data) VALUES(:id,:data) ON CONFLICT DO NOTHING",
                        mapOf(
                            "id" to internId,
                            "data" to
                                PGobject().apply {
                                    type = "jsonb"
                                    value = it.data().toString()
                                },
                        ),
                    ),
                )
            }
        }

        override fun visitPerson(
            navn: String,
            aktørId: String,
            ident: String,
            norskTilknytning: Boolean,
            diskresjonskode: Boolean,
            egenAnsatt: Boolean,
        ) {
            lagreQueries.add(
                queryOf(
                    //language=PostgreSQL
                    """
                       INSERT INTO person_v1(ident, aktørId)
                        VALUES(:ident,:aktoerId) 
                        ON CONFLICT DO NOTHING
                    """.trimMargin(),
                    mapOf(
                        "id" to internId,
                        "ident" to ident,
                        "aktoerId" to aktørId,
                    ),
                ),
            )

            lagreQueries.add(
                queryOf(
                    //language=PostgreSQL
                    """
                        INSERT INTO person_innsending_v1(id,navn,personId,norsktilknytning,diskresjonskode,egenansatt) 
                        SELECT :id, :navn, id, :norskTilknytning, :diskresjonskode,:egenansatt
                        FROM public.person_v1 WHERE ident = :ident 
                        AND aktørid = :aktoerId ON CONFLICT DO NOTHING 
                    """.trimMargin(),
                    mapOf(
                        "id" to internId,
                        "ident" to ident,
                        "navn" to navn,
                        "aktoerId" to aktørId,
                        "norskTilknytning" to norskTilknytning,
                        "diskresjonskode" to diskresjonskode,
                        "egenansatt" to egenAnsatt,
                    ),
                ),
            )
        }

        override fun visitArenaSak(
            oppgaveId: String,
            fagsakId: String?,
        ) {
            lagreQueries.add(
                queryOf(
                    //language=PostgreSQL
                    """
                        INSERT INTO arenasak_v1(fagsakId, oppgaveId, id) VALUES (:fagsakId, :oppgaveId, :id) 
                        ON CONFLICT(id) DO NOTHING 
                        """,
                    mapOf(
                        "id" to internId,
                        "fagsakId" to fagsakId,
                        "oppgaveId" to oppgaveId,
                    ),
                ),
            )
        }

        override fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            lagreQueries.add(
                queryOf(
                    //language=PostgreSQL
                    """
                    INSERT INTO aktivitetslogg_v1(id, data) 
                    VALUES (:id, :data) ON CONFLICT(id) DO UPDATE SET data=:data 
                    """.trimIndent(),
                    mapOf(
                        "id" to internId,
                        "data" to
                            PGobject().apply {
                                type = "jsonb"
                                value = JsonMapper.jacksonJsonAdapter.writeValueAsString(aktivitetslogg.toMap())
                            },
                    ),
                ),
            )
        }
    }
}
