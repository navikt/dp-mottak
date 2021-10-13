package no.nav.dagpenger.mottak

import mu.KotlinLogging
import no.nav.dagpenger.mottak.api.innsendingApi
import no.nav.dagpenger.mottak.behov.eksterne.PostgresSøknadQuizOppslag
import no.nav.dagpenger.mottak.behov.eksterne.SøknadFaktaQuizLøser
import no.nav.dagpenger.mottak.behov.journalpost.FerdigstillJournalpostBehovLøser
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApiClient
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostBehovLøser
import no.nav.dagpenger.mottak.behov.journalpost.OppdaterJournalpostBehovLøser
import no.nav.dagpenger.mottak.behov.journalpost.SafClient
import no.nav.dagpenger.mottak.behov.journalpost.SøknadsdataBehovLøser
import no.nav.dagpenger.mottak.behov.person.PdlPersondataOppslag
import no.nav.dagpenger.mottak.behov.person.PersondataBehovLøser
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaApiClient
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaBehovLøser
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysProxyClient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.OpprettGosysOppgaveLøser
import no.nav.dagpenger.mottak.behov.vilkårtester.MinsteinntektVurderingLøser
import no.nav.dagpenger.mottak.behov.vilkårtester.RegelApiProxy
import no.nav.dagpenger.mottak.db.InnsendingPostgresRepository
import no.nav.dagpenger.mottak.db.MinsteinntektVurderingPostgresRepository
import no.nav.dagpenger.mottak.db.runMigration
import no.nav.dagpenger.mottak.observers.FerdigstiltInnsendingObserver
import no.nav.dagpenger.mottak.observers.InnsendingProbe
import no.nav.dagpenger.mottak.observers.MetrikkObserver
import no.nav.dagpenger.mottak.tjenester.JoarkHendelseMottak
import no.nav.dagpenger.mottak.tjenester.MottakMediator
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

private val logg = KotlinLogging.logger {}

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    private val innsendingRepository = InnsendingPostgresRepository(Config.dataSource)
    private val safClient = SafClient(Config.properties)
    private val regelApiClient = RegelApiProxy(Config.properties)
    private val arenaApiClient = ArenaApiClient(Config.properties)
    private val journalpostApiClient = JournalpostApiClient(Config.properties)
    private val gosysProxyClient = GosysProxyClient(Config.properties)
    private val minsteinntektVurderingRepository = MinsteinntektVurderingPostgresRepository(Config.dataSource)
    private val ferdigstiltInnsendingObserver = FerdigstiltInnsendingObserver(Config.kafkaConfig)

    private val rapidsConnection = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(env)
    )
        .also { builder ->
            Config.basicCredentials?.let {
                builder.withKtorModule(
                    innsendingApi(
                        innsendingRepository,
                        ferdigstiltInnsendingObserver,
                        it
                    )
                )
            }
        }
        .build()
    private val mediator = InnsendingMediator(
        innsendingRepository = innsendingRepository,
        rapidsConnection = rapidsConnection,
        observatører = listOf(
            ferdigstiltInnsendingObserver,
            MetrikkObserver(),
            InnsendingProbe
        )
    )
    private val joarkHendelseMottak =
        JoarkHendelseMottak(Config.journalføringTopic, Config.kafkaAvroConsumerProperties, mediator)

    init {
        // Behovmottakere
        MottakMediator(mediator, rapidsConnection)

        // Behovløsere
        JournalpostBehovLøser(safClient, rapidsConnection)
        OppdaterJournalpostBehovLøser(journalpostApiClient, rapidsConnection)
        FerdigstillJournalpostBehovLøser(journalpostApiClient, rapidsConnection)
        PersondataBehovLøser(PdlPersondataOppslag(Config.properties), rapidsConnection)
        SøknadsdataBehovLøser(safClient, rapidsConnection)
        MinsteinntektVurderingLøser(
            regelApiClient = regelApiClient,
            repository = minsteinntektVurderingRepository,
            rapidsConnection = rapidsConnection
        )
        ArenaBehovLøser(arenaApiClient, rapidsConnection)
        OpprettGosysOppgaveLøser(gosysProxyClient, rapidsConnection)

        // Eksterne behovløsere
        SøknadFaktaQuizLøser(PostgresSøknadQuizOppslag(Config.dataSource), rapidsConnection)

        rapidsConnection.register(this)
    }

    fun start() {
        rapidsConnection.start()
        joarkHendelseMottak.start()
    }

    override fun onStartup(rapidsConnection: RapidsConnection) {
        runMigration(Config.dataSource)
        logg.info { "Starter dp-mottak" }
    }

    override fun onShutdown(rapidsConnection: RapidsConnection) {
        joarkHendelseMottak.stop()
    }
}
