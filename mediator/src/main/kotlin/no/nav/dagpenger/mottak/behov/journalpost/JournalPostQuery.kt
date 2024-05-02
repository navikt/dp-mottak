package no.nav.dagpenger.mottak.behov.journalpost

import com.fasterxml.jackson.annotation.JsonIgnore
import no.nav.dagpenger.mottak.behov.GraphqlQuery

internal data class JournalPostQuery(
    @JsonIgnore val journalpostId: String,
) : GraphqlQuery(
        //language=Graphql
        query =
            """ 
            query(${'$'}journalpostId: String!) {
                journalpost(journalpostId: ${'$'}journalpostId) {
                    journalstatus
                    journalpostId
                    journalfoerendeEnhet
                    datoOpprettet
                    behandlingstema
                    bruker {
                      type
                      id
                    }
                    relevanteDatoer {
                      dato
                      datotype
                    }
                    dokumenter {
                      tittel
                      dokumentInfoId
                      brevkode
                    }
                }
            }
            """.trimIndent(),
        variables =
            mapOf(
                "journalpostId" to journalpostId,
            ),
    )
