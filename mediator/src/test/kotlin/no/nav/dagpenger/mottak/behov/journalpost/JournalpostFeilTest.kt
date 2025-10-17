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
    }

    @Suppress("ktlint:standard:max-line-length")
    //language=JSON
    private val json = """
                    {
                      "message": "Kunne ikke oppdatere journalpost med journalpostId=723892526. bruker kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I, sak kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I, journalfoerendeEnhet kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I, tema kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I"
                    }
            """
}
