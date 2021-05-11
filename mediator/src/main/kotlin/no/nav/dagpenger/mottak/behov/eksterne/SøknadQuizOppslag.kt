package no.nav.dagpenger.mottak.behov.eksterne

import no.nav.dagpenger.mottak.SøknadFakta

internal interface SøknadQuizOppslag {
    fun hentSøknad(journalpostId: String): SøknadFakta
}
