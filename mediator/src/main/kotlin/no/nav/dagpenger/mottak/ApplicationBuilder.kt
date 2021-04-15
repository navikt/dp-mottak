package no.nav.dagpenger.mottak

import mu.KotlinLogging
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostBehovLøser
import no.nav.dagpenger.mottak.behov.journalpost.SafClient
import no.nav.dagpenger.mottak.behov.journalpost.SøknadsdataBehovLøser
import no.nav.dagpenger.mottak.behov.person.PdlPersondataOppslag
import no.nav.dagpenger.mottak.behov.person.PersondataBehovLøser
import no.nav.dagpenger.mottak.behov.vilkårtester.MinsteinntektVurderingLøser
import no.nav.dagpenger.mottak.behov.vilkårtester.RegelApiProxy
import no.nav.dagpenger.mottak.db.InMemoryInnsendingRepository
import no.nav.dagpenger.mottak.proxy.proxyPing
import no.nav.dagpenger.mottak.tjenester.MottakMediator
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

private val logg = KotlinLogging.logger {}

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    private val innsendingRepository = InMemoryInnsendingRepository()
    private val safClient = SafClient(Config.properties)
    private val regelApiClient = RegelApiProxy(Config.properties)

    private val rapidsConnection = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(env)
    )
        .withKtorModule(proxyPing(Config.properties))
        .build().apply {
            val mediator = InnsendingMediator(innsendingRepository = innsendingRepository, rapidsConnection = this)
            // Behovmottakere
            MottakMediator(mediator, this)

            // Behovløsere
            JournalpostBehovLøser(safClient, this)
            PersondataBehovLøser(PdlPersondataOppslag(Config.properties), this)
            SøknadsdataBehovLøser(safClient, this)
            MinsteinntektVurderingLøser(regelApiClient, this)
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logg.info { "Starter dp-mottak" }
    }
}
