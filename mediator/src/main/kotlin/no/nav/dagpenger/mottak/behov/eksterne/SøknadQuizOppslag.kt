package no.nav.dagpenger.mottak.behov.eksterne

import kotliquery.queryOf
import kotliquery.sessionOf
import kotliquery.using
import no.nav.dagpenger.mottak.SøknadFaktum
import no.nav.dagpenger.mottak.behov.JsonMapper
import no.nav.dagpenger.mottak.meldinger.Søknadsdata
import javax.sql.DataSource

internal interface SøknadQuizOppslag {
    fun hentSøknad(innsendtSøknadsId: String): SøknadFaktum
}

internal class PostgresSøknadQuizOppslag(private val dataSource: DataSource) : SøknadQuizOppslag {

    override fun hentSøknad(innsendtSøknadsId: String): SøknadFaktum {
        // TODO: bruke fnr? (brukerbehandligId skal være unik, vil evt være et safety measure for å være helt sikker på at bruker ikke får feil søknad)
        val query = queryOf(
            "SELECT * FROM soknad_v1 WHERE data ->> 'brukerBehandlingId' = :id",
            mapOf("id" to innsendtSøknadsId)
        )

        return using(sessionOf(dataSource)) { session ->
            session.run(
                query.map { row ->
                    row.binaryStreamOrNull("data")?.use {
                        Søknadsdata.GammelSøknad(JsonMapper.jacksonJsonAdapter.readTree(it))
                    }
                }.asSingle
            ) ?: throw IllegalArgumentException("Fant ikke søknad med innsendtId $innsendtSøknadsId")
        }
    }
}
