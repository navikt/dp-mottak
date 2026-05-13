package no.nav.dagpenger.mottak.behov.journalpost

import io.github.oshai.kotlinlogging.KotlinLogging
import no.nav.dagpenger.mottak.defaultObjectMapper

internal interface JournalpostFeil {
    private companion object {
        val logger = KotlinLogging.logger { }
        private val whitelistFeilmeldinger =
            setOf(
                "Bruker kan ikke oppdateres for journalpost med journalpostStatus=J og journalpostType=I.",
                "er ikke midlertidig journalført",
                "er ikke midlertidig journalf&oslash;rt",
            )
        private val nyWhitelistFeilmeldinger =
            setOf(
                "bruker kan ikke oppdateres for journalpost med journalpoststatus=J og journalposttype=I",
            )
    }

    class JournalpostException(val statusCode: Int, val content: String?) : RuntimeException()

    fun ignorerKjenteTilstander(journalpostException: JournalpostException) {
        when (journalpostException.statusCode) {
            400 -> {
                logger.info { "CONTENT -> ${journalpostException.content}" }
                val json = defaultObjectMapper.readTree(journalpostException.content)

                val feilmelding = json["message"].asString()

                when {
                    feilmelding in whitelistFeilmeldinger -> {
                        return
                    }

                    whitelistFeilmeldinger.any { feilmelding.endsWith(it) } -> {
                        return
                    }

                    whitelistFeilmeldinger.any { feilmelding.startsWith(it) } -> {
                        return
                    }

                    nyWhitelistFeilmeldinger.any { feilmelding.contains(it) } -> {
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
