package no.nav.dagpenger.mottak.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Config
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingVisitor
import no.nav.dagpenger.mottak.behov.JsonMapper
import no.nav.dagpenger.mottak.meldinger.Journalpost
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import no.nav.dagpenger.mottak.serder.InnsendingData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerData
import no.nav.dagpenger.mottak.serder.InnsendingData.JournalpostData.BrukerTypeData
import no.nav.dagpenger.mottak.serder.InnsendingData.TilstandData
import no.nav.dagpenger.mottak.toMap
import org.intellij.lang.annotations.Language
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import javax.sql.DataSource

internal class InnsendingPostgresRepository(private val datasource: DataSource = Config.dataSource) :
    InnsendingRepository {
    @Language("PostgreSQL")
    val hentDataSql = """
     select innsending.id                             as "internId",
            innsending.journalpostid                  as "journalpostId",
            innsending.tilstand                       as "tilstand",
            innsending.opprettet                      as "opprettet",
            journalpost.status                        as "status",
            journalpost.brukerid                      as "brukerId",
            journalpost.brukertype                    as "brukerType",
            journalpost.behandlingstema               as "behandlingstema",
            journalpost.registrertdato                as "registrertdato",
            arenasak.fagsakid                         as "fagsakId",
            arenasak.oppgaveId                        as "oppgaveId",
            innsending_eksisterende_arena_saker.verdi as "harEksisterendeSaker",
            innsending_oppfyller_minsteinntekt.verdi  as "oppfyllerMinsteArbeidsinntekt",
            soknad.data                               as "søknadsData",
            person_innsending.diskresjonskode         as "diskresjonsKode",
            person_innsending.norsktilknytning        as "norsktilknytning",
            person.fødselsnummer                      as "fødselsnummer",
            person.aktørid                            as "aktørid",
            aktivitetslogg.data                       as "aktivitetslogg"
     from innsending_v1 as innsending
              left join journalpost_v1 journalpost on innsending.id = journalpost.id
              left join aktivitetslogg_v1 aktivitetslogg on innsending.id = aktivitetslogg.id
              left join arenasak_v1 arenasak on innsending.id = arenasak.id
              left join innsending_eksisterende_arena_saker_v1 innsending_eksisterende_arena_saker
                        on innsending.id = innsending_eksisterende_arena_saker.id
              left join innsending_oppfyller_minsteinntekt_v1 innsending_oppfyller_minsteinntekt
                        on innsending.id = innsending_oppfyller_minsteinntekt.id
              left join soknad_v1 soknad on innsending.id = soknad.id
              left join person_innsending_v1 person_innsending on innsending.id = person_innsending.id
              left join person_v1 person on person_innsending.personid = person.id
     where innsending.journalpostId = :jpId;
    """.trimIndent()

    fun Row.booleanOrNull(columnLabel: String) = this.stringOrNull(columnLabel)?.let { it == "t" }

    override fun hent(journalpostId: String): Innsending? =
        using(sessionOf(datasource)) { session ->
            session.run(
                queryOf(
                    hentDataSql,
                    mapOf("jpId" to journalpostId.toLong())
                ).map { row ->
                    InnsendingData(
                        id = row.long("internId"),
                        journalpostId = row.long("journalpostId").toString(),
                        tilstand = TilstandData(TilstandData.InnsendingTilstandTypeData.valueOf(row.string("tilstand"))),
                        journalpostData = row.localDateTimeOrNull("registrertDato")?.let {
                            InnsendingData.JournalpostData(
                                journalpostId = row.long("journalpostId").toString(),
                                bruker = row.stringOrNull("brukerType")?.let { type ->
                                    BrukerData(BrukerTypeData.valueOf(type), row.string("brukerId"))
                                },
                                journalpostStatus = row.string("status"),
                                behandlingstema = row.stringOrNull("behandlingstema"),
                                registertDato = it,
                                dokumenter = listOf()
                            )
                        },
                        oppfyllerMinsteArbeidsinntekt = row.booleanOrNull("oppfyllerMinsteArbeidsinntekt"),
                        eksisterendeSaker = row.booleanOrNull("harEksisterendeSaker"),
                        personData = row.stringOrNull("fødselsnummer")?.let {
                            InnsendingData.PersonData(
                                aktørId = row.string("aktørId"),
                                fødselsnummer = row.string("fødselsnummer"),
                                norskTilknytning = row.boolean("norsktilknytning"),
                                diskresjonskode = row.boolean("diskresjonskode")
                            )
                        },
                        arenaSakData = row.stringOrNull("fagsakId")?.let {
                            InnsendingData.ArenaSakData(
                                oppgaveId = row.string("oppgaveId"),
                                fagsakId = it
                            )
                        },
                        søknadsData = row.binaryStreamOrNull("søknadsData")?.use {
                            JsonMapper.jacksonJsonAdapter.readTree(it)
                        },
                        aktivitetslogg = row.binaryStream("aktivitetslogg").use {
                            JsonMapper.jacksonJsonAdapter.readValue(
                                it,
                                InnsendingData.AktivitetsloggData::class.java
                            )
                        }
                    )
                }.asSingle
            )?.let {
                val dokumenter = session.run(
                    queryOf( //language=PostgreSQL
                        """
                            SELECT 
                            brevkode,
                            tittel,
                            dokumentinfoid
                            FROM journalpost_dokumenter_v1 WHERE id = :internId
                        """.trimIndent(),
                        mapOf(
                            "internId" to it.id
                        )

                    ).map { row ->
                        InnsendingData.JournalpostData.DokumentInfoData(
                            brevkode = row.string("brevkode"),
                            tittel = row.string("tittel"),
                            dokumentInfoId = row.string("dokumentInfoId")
                        )
                    }.asList
                )

                it.copy(journalpostData = it.journalpostData?.copy(dokumenter = dokumenter)).createInnsending()
            }
        }

    override fun lagre(innsending: Innsending): Int {

        val internId: Long = using(sessionOf(datasource)) { session ->
            session.run(
                queryOf( //language=PostgreSQL
                    """ 
                        SELECT id from innsending_v1 where journalpostid = :journalpostId
                    """.trimIndent(),
                    mapOf("journalpostId" to innsending.journalpostId().toLong())
                ).map {
                    it.longOrNull("id")
                }.asSingle
            ) ?: NyInnsendingQueryVisiotor(innsending, datasource).internId
        }
        val visitor = InnsendingQueryVisitor(innsending, internId)
        return using(sessionOf(datasource)) { session ->
            session.transaction { tx -> visitor.lagreQueries.map { tx.run(it.asUpdate) }.sum() }
        }
    }

    class NyInnsendingQueryVisiotor(private val innsending: Innsending, private val datasource: DataSource) :
        InnsendingVisitor {
        var internId: Long = 0

        init {
            innsending.accept(this)
        }

        override fun visitTilstand(tilstandType: Innsending.Tilstand) {
            internId = using(sessionOf(datasource)) { session ->
                session.transaction {
                    it.run(
                        queryOf( //language=Postgresql
                            "INSERT INTO innsending_v1(journalpostId,tilstand) VALUES(:jpId, :tilstand) RETURNING id",
                            mapOf(
                                "jpId" to innsending.journalpostId().toLong(),
                                "tilstand" to tilstandType.type.name
                            )
                        ).map { row ->
                            row.long("id")
                        }.asSingle
                    )
                }
            } ?: throw IllegalArgumentException("Feil ved opprettelse av Innsending! Noe er galt")
        }
    }

    class InnsendingQueryVisitor(innsending: Innsending, private val internId: Long) : InnsendingVisitor {

        val lagreQueries: MutableList<Query> = mutableListOf()

        init {
            innsending.accept(this)
        }

        override fun visitTilstand(tilstandType: Innsending.Tilstand) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    """
                        INSERT INTO innsending_v1(id,tilstand) VALUES(:id, :tilstand) 
                        ON CONFLICT(id) DO UPDATE  set tilstand = :tilstand
                    """.trimIndent(),
                    mapOf(
                        "id" to internId,
                        "tilstand" to tilstandType.type.name
                    )
                )
            )
        }

        override fun visitInnsending(oppfyllerMinsteArbeidsinntekt: Boolean?, eksisterendeSaker: Boolean?) {
            oppfyllerMinsteArbeidsinntekt?.let {
                lagreQueries.add(
                    queryOf( //language=PostgreSQL
                        "INSERT INTO innsending_oppfyller_minsteinntekt_v1(id,verdi) VALUES (:id, :verdi) ON CONFLICT DO NOTHING ",
                        mapOf(
                            "id" to internId,
                            "verdi" to it
                        )
                    )
                )
            }
            eksisterendeSaker?.let {
                lagreQueries.add(
                    queryOf( //language=PostgreSQL
                        "INSERT INTO innsending_eksisterende_arena_saker_v1(id,verdi) VALUES (:id, :verdi) ON CONFLICT DO NOTHING ",
                        mapOf(
                            "id" to internId,
                            "verdi" to it
                        )
                    )
                )
            }
        }

        override fun visitJournalpost(
            journalpostId: String,
            journalpostStatus: String,
            bruker: Journalpost.Bruker?,
            behandlingstema: String?,
            registrertDato: LocalDateTime,
            dokumenter: List<Journalpost.DokumentInfo>
        ) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    """
                        INSERT INTO journalpost_v1(id,status, brukerId,brukerType,behandlingstema,registrertDato) 
                        VALUES(:id, :status,:brukerId, :brukerType, :behandlingstema,:registrertDato)
                        ON CONFLICT(id) DO NOTHING 
                    """.trimIndent(),
                    mapOf(
                        "id" to internId,
                        "status" to journalpostStatus,
                        "brukerId" to bruker?.id,
                        "brukerType" to bruker?.type?.name,
                        "behandlingstema" to behandlingstema,
                        "registrertDato" to registrertDato
                    )
                )
            )
            val dokumentliste =
                dokumenter.joinToString { """('$internId','${it.tittel}','${it.dokumentInfoId}','${it.brevkode}')""" }

            if (dokumentliste.isNotEmpty()) {
                lagreQueries.add(
                    queryOf( //language=PostgreSQL
                        """
                            INSERT INTO journalpost_dokumenter_v1(id,tittel,dokumentInfoId,brevkode) 
                            VALUES $dokumentliste ON CONFLICT DO NOTHING
                             
                        """.trimIndent()
                    )
                )
            }
        }

        override fun visitSøknad(søknad: Søknadsdata.Søknad?) {
            søknad?.let {
                lagreQueries.add(
                    queryOf( //language=PostgreSQL
                        "INSERT INTO soknad_v1(id,data) VALUES(:id,:data) ON CONFLICT DO NOTHING",
                        mapOf(
                            "id" to internId,
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
                    """
                       INSERT INTO person_v1(fødselsnummer,aktørId)
                        VALUES(:fnr,:aktoerId) 
                        ON CONFLICT DO NOTHING
                        """.trimMargin(),
                    mapOf(
                        "id" to internId,
                        "fnr" to fødselsnummer,
                        "aktoerId" to aktørId,
                    )
                )
            )

            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    """
                        INSERT INTO person_innsending_v1(id,personId,norsktilknytning,diskresjonskode) 
                        SELECT :id, id, :norskTilknytning, :diskresjonskode
                        FROM public.person_v1 WHERE fødselsnummer = :fnr 
                        AND aktørid = :aktoerId ON CONFLICT DO NOTHING 
                        """.trimMargin(),
                    mapOf(
                        "id" to internId,
                        "fnr" to fødselsnummer,
                        "aktoerId" to aktørId,
                        "norskTilknytning" to norskTilknytning,
                        "diskresjonskode" to diskresjonskode
                    )
                )
            )
        }

        override fun visitArenaSak(oppgaveId: String?, fagsakId: String) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    """
                        INSERT INTO arenasak_v1(fagsakId, oppgaveId, id) VALUES (:fagsakId, :oppgaveId, :id) 
                        ON CONFLICT(id) DO NOTHING 
                        """,
                    mapOf(
                        "id" to internId,
                        "fagsakId" to fagsakId,
                        "oppgaveId" to oppgaveId
                    )
                )
            )
        }

        override fun preVisitAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    """
                            INSERT INTO aktivitetslogg_v1(id, data) 
                            VALUES (:id, :data) ON CONFLICT(id) DO UPDATE SET data=:data 
                    """.trimIndent(),
                    mapOf(
                        "id" to internId,
                        "data" to PGobject().apply {
                            type = "jsonb"
                            value = JsonMapper.jacksonJsonAdapter.writeValueAsString(aktivitetslogg.toMap())
                        }
                    )
                )
            )
        }
    }
}
