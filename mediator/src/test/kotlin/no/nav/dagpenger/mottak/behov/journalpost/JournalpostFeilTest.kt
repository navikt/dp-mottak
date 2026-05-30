package no.nav.dagpenger.mottak.behov.journalpost

import org.junit.jupiter.api.Test

class JournalpostFeilTest {
    private val testObject = object : JournalpostFeil {}

    @Test
    fun testIgnorerKjenteTilstander() {
        val testObject = object : JournalpostFeil {}

        testObject.ignorerKjenteTilstander(
            JournalpostFeil.JournalpostException(
                statusCode = 400,
                content = json.trimIndent(),
            ),
        )

        testObject.ignorerKjenteTilstander(
            JournalpostFeil.JournalpostException(
                statusCode = 400,
                content = enAnnenFeil.trimIndent(),
            ),
        )
    }

    @Suppress("ktlint:standard:max-line-length")
    //language=JSON
    private val json = """
                    {
                      "message": "Kunne ikke oppdatere journalpost med journalpostId=723892526. bruker kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I, sak kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I, journalfoerendeEnhet kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I, tema kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I"
                    }
            """

    //language=JSON
    private val enAnnenFeil =
        """
        {
          "type": "about:blank",
          "title": "Bad Request",
          "status": 400,
          "detail": "Kunne ikke ferdigstille journalpost med journalpostId=754135467. Journalposten er ikke ansett som midlertidig journalført av følgende grunn(er): Den har journalstatus=J (midlertidig journalførte journalposter har en av følgende journalstatuser=[M, U, D, R, FS, FL, A, MO, UB, OD])",
          "instance": "/rest/journalpostapi/v1/journalpost/754135467/ferdigstill",
          "timestamp": "2026-05-30T15:01:05.504264364+02:00",
          "message": "Kunne ikke ferdigstille journalpost med journalpostId=754135467. Journalposten er ikke ansett som midlertidig journalført av følgende grunn(er): Den har journalstatus=J (midlertidig journalførte journalposter har en av følgende journalstatuser=[M, U, D, R, FS, FL, A, MO, UB, OD])",
          "error": "Bad Request",
          "path": "/rest/journalpostapi/v1/journalpost/754135467/ferdigstill"
        }
        """.trimIndent()
}
