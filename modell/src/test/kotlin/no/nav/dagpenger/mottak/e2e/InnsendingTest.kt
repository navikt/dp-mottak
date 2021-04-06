package no.nav.dagpenger.mottak

import no.nav.dagpenger.mottak.e2e.AbstractEndeTilEndeTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

internal class InnsendingTest : AbstractEndeTilEndeTest() {

    @ParameterizedTest
    @ValueSource(strings = ["NAV 04-01.03", "NAV 04-01.04"])
    fun `skal håndtere joark hendelse der journalpost er ny søknad`(brevkode: String) {
        håndterJoarkHendelse()

        håndterJournalpostData(brevkode)

        håndterPersonInformasjon()

        håndterSøknadsdata()

        håndterMinsteinntektVurderingData()

        håndterEksisterendesakData()

        håndterArenaOppgaveOpprettet()

        håndterJournalpostOppdatert()

        håndterJournalpostFerdigstilt()

        assertTilstander(
            InnsendingTilstandType.MottattType,
            InnsendingTilstandType.AvventerJournalpostType,
            InnsendingTilstandType.AvventerPersondataType,
            InnsendingTilstandType.KategoriseringType,
            InnsendingTilstandType.AvventerSøknadsdataType,
            InnsendingTilstandType.AvventerMinsteinntektVurderingType,
            InnsendingTilstandType.AvventerSvarOmEksisterendeSakerType,
            InnsendingTilstandType.AventerArenaStartVedtakType,
            InnsendingTilstandType.OppdaterJournalpostType,
            InnsendingTilstandType.FerdigstillJournalpostType,
            InnsendingTilstandType.JournalførtType
        )

        inspektør.also {
            assertNoErrors(it)
            assertMessages(it)
            println(it.innsendingLogg.toString())
        }
    }
}
