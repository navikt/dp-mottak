package no.nav.dagpenger.mottak.db

import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingPeriode
import no.nav.dagpenger.mottak.api.Periode
import java.util.UUID

interface InnsendingRepository {
    fun hent(journalpostId: String): Innsending?

    fun lagre(innsending: Innsending): Int

    fun forPeriode(periode: Periode): List<InnsendingPeriode>
}

interface InnsendingMetadataRepository {
    fun hentOppgaverIder(
        søknadId: UUID,
        ident: String,
    ): List<String>
}

/*
select    aren.oppgaveid
from      innsending_v1 inns
join      person_innsending_v1 peri on inns.id = peri.id
join      person_v1 pers          on pers.id = peri.personid
join soknad_v1 sokn          on inns.id = sokn.id
join arenasak_v1 aren on inns.id = aren.id
where 1=1
and pers.ident = '190381%'
and sokn.data ->> '@id' = '49b7154a-64a1-43cd-a232-06aad45a3131';*/
