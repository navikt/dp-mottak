package no.nav.dagpenger.mottak.behov.saksbehandling

import no.nav.helse.rapids_rivers.JsonMessage
import java.time.LocalDate
import java.util.UUID

interface VedtakOppslagApi {
    suspend fun hentEksisterendeSaker(fødselsnummer: String, journalpostId: String)
}

internal class VedtakOppslag {

    data class DpIdent(val type: String, val idVerdi: String, val historisk: Boolean)

}

//