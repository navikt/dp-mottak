package no.nav.dagpenger.mottak

import mu.KotlinLogging
import no.nav.dagpenger.mottak.db.InMemoryInnsendingRepository
import no.nav.dagpenger.mottak.proxy.proxyPing
import no.nav.dagpenger.mottak.tjenester.JournalføringMottak
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

private val logg = KotlinLogging.logger {}

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    private val innsendingRepository = InMemoryInnsendingRepository()

    private val rapidsConnection = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(env)
    )
        .withKtorModule(proxyPing(Configuration.properties))
        .build().apply {
            val mediator = InnsendingMediator(innsendingRepository = innsendingRepository, rapidsConnection = this)
            JournalføringMottak(mediator, this)
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        logg.info { "Starter dp-mottak" }
    }
}
