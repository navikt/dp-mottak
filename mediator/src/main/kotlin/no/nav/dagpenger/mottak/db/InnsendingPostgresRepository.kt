package no.nav.dagpenger.mottak.db

import kotliquery.Query
import kotliquery.Row
import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
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
import org.postgresql.util.PGobject
import java.time.LocalDateTime
import javax.sql.DataSource

internal class InnsendingPostgresRepository(private val datasource: DataSource = Config.dataSource) :
    InnsendingRepository {
    val hentDataSql = """
     select innsending.journalpostid                  as "journalpostId",
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
              left join journalpost_v1 journalpost on innsending.journalpostid = journalpost.journalpostid
              left join aktivitetslogg aktivitetslogg on innsending.journalpostid = aktivitetslogg.journalpostid
              left join arenasak_v1 arenasak on innsending.journalpostid = arenasak.journalpostid
              left join innsending_eksisterende_arena_saker_v1 innsending_eksisterende_arena_saker
                        on innsending.journalpostid = innsending_eksisterende_arena_saker.journalpostid
              left join innsending_oppfyller_minsteinntekt_v1 innsending_oppfyller_minsteinntekt
                        on innsending.journalpostid = innsending_oppfyller_minsteinntekt.journalpostid
              left join soknad_v1 soknad on innsending.journalpostid = soknad.journalpostid
              left join person_innsending_v1 person_innsending on innsending.journalpostid = person_innsending.journalpostid
              left join person_v1 person on person_innsending.personid = person.id
     where innsending.journalpostid = :jpId;
 """.trimIndent()

    fun Row.booleanOrNull(columnLabel: String) = this.stringOrNull(columnLabel)?.let { it == "t" }

    override fun hent(journalpostId: String): Innsending =
        using(sessionOf(datasource)) { session ->
            session.run(
                queryOf(
                    hentDataSql,
                    mapOf("jpId" to journalpostId.toLong())
                ).map { row ->
                    InnsendingData(
                        journalpostId = row.int("journalpostId").toString(),
                        tilstand = TilstandData(TilstandData.InnsendingTilstandTypeData.valueOf(row.string("tilstand"))),
                        journalpostData = row.localDateTimeOrNull("registrertDato")?.let {
                            InnsendingData.JournalpostData(
                                journalpostId = row.int("journalpostId").toString(),
                                bruker = row.stringOrNull("brukerType")?.let {
                                    BrukerData(BrukerTypeData.valueOf(it), row.string("brukerId"))
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
                        aktivitetslogg = InnsendingData.AktivitetsloggData(listOf()), //todo
                        arenaSakData = row.stringOrNull("fagsakId")?.let {
                            InnsendingData.ArenaSakData(
                                oppgaveId = row.string("oppgaveId"),
                                fagsakId = it
                            )
                        },
                        søknadsData = row.stringOrNull("søknadsData")?.let {
                            JsonMapper.jacksonJsonAdapter.readTree(it)
                        }
                    ).createInnsending()
                }.asSingle
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
            registrertDato: LocalDateTime,
            dokumenter: List<Journalpost.DokumentInfo>
        ) {
            lagreQueries.add(
                queryOf( //language=PostgreSQL
                    "INSERT INTO journalpost_v1(journalpostId,status, brukerId,brukerType,behandlingstema,registrertDato) VALUES(:jpId, :status,:brukerId, :brukerType, :behandlingstema,:registrertDato)",
                    mapOf(
                        "jpId" to jpId,
                        "status" to journalpostStatus,
                        "brukerId" to bruker?.id,
                        "brukerType" to bruker?.type?.name,
                        "behandlingstema" to behandlingstema,
                        "registrertDato" to registrertDato
                    )
                )
            )
            val dokumentliste =
                dokumenter.joinToString { """('$jpId','${it.tittel}','${it.dokumentInfoId}','${it.brevkode}')""" }

            if (dokumentliste.isNotEmpty()) {
                lagreQueries.add(
                    queryOf( //language=PostgreSQL
                        "INSERT INTO journalpost_dokumenter_v1(journalpostId,tittel,dokumentInfoId,brevkode) VALUES $dokumentliste"
                    )
                )
            }
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
                    """INSERT INTO arenasak_v1(fagsakId, oppgaveId, journalpostId)
VALUES (:fagsakId, :oppgaveId, :journalpostId) 
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
