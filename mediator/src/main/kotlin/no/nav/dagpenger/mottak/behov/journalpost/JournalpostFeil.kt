package no.nav.dagpenger.mottak.behov.journalpost

import mu.KotlinLogging
import no.nav.dagpenger.mottak.behov.JsonMapper

internal interface JournalpostFeil {
    private companion object {
        val logger = KotlinLogging.logger { }
        private val whitelistFeilmeldinger =
            setOf(
                "Bruker kan ikke oppdateres for journalpost med journalpostStatus=J og journalpostType=I.",
                "er ikke midlertidig journalført",
                "er ikke midlertidig journalf&oslash;rt",
            )
    }

    class JournalpostException(val statusCode: Int, val content: String?) : RuntimeException()

    fun ignorerKjenteTilstander(journalpostException: JournalpostException) {
        when (journalpostException.statusCode) {
            400 -> {
                logger.info { "CONTENT -> ${journalpostException.content}" }
                val json = JsonMapper.jacksonJsonAdapter.readTree(journalpostException.content)

                val feilmelding = json["message"].asText()

                when {
                    feilmelding in whitelistFeilmeldinger -> {
                        return
                    }
                    whitelistFeilmeldinger.any { feilmelding.endsWith(it) } -> {
                        return
                    }
                    else -> {
                        throw journalpostException
                    }
                }
            }
            else -> {
                throw journalpostException
            }
        }
    }
}
