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
import no.nav.dagpenger.mottak.db.PostgresDataSourceBuilder
import no.nav.dagpenger.mottak.observers.FerdigstiltInnsendingObserver
import no.nav.dagpenger.mottak.observers.InnsendingProbe
import no.nav.dagpenger.mottak.observers.MetrikkObserver
import no.nav.dagpenger.mottak.tjenester.MottakMediator
import no.nav.dagpenger.mottak.tjenester.SøknadsDataVaktmester
import no.nav.dagpenger.mottak.tjenester.SøknadsDataVaktmester.Companion.fixManglendeSøknadsData
import no.nav.helse.rapids_rivers.RapidApplication
import no.nav.helse.rapids_rivers.RapidsConnection

private val logg = KotlinLogging.logger {}

internal class ApplicationBuilder(env: Map<String, String>) : RapidsConnection.StatusListener {

    private val innsendingRepository = InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)
    private val safClient = SafClient(Config.properties)
    private val regelApiClient = RegelApiProxy(Config.properties)
    private val arenaApiClient = ArenaApiClient(Config.properties)
    private val journalpostApiClient = JournalpostApiClient(Config.properties)
    private val gosysProxyClient = GosysProxyClient(Config.properties)
    private val minsteinntektVurderingRepository =
        MinsteinntektVurderingPostgresRepository(PostgresDataSourceBuilder.dataSource)
    private val ferdigstiltInnsendingObserver = FerdigstiltInnsendingObserver(Config.kafkaProducerProperties)
    private val søknadsDataVaktmester = SøknadsDataVaktmester(safClient)

    private val rapidsConnection = RapidApplication.Builder(
        RapidApplication.RapidApplicationConfig.fromEnv(env)
    )
        .also { builder ->
            builder.withKtorModule {
                innsendingApi(
                    innsendingRepository,
                    ferdigstiltInnsendingObserver,

                )
            }
        }
        .build().apply {
            val mediator = InnsendingMediator(
                innsendingRepository = innsendingRepository,
                rapidsConnection = this,
                observatører = listOf(
                    ferdigstiltInnsendingObserver,
                    MetrikkObserver(),
                    InnsendingProbe
                )
            )
            // Behovmottakere
            MottakMediator(mediator, this)

            // Behovløsere
            JournalpostBehovLøser(safClient, this)
            OppdaterJournalpostBehovLøser(journalpostApiClient, this)
            FerdigstillJournalpostBehovLøser(journalpostApiClient, this)
            PersondataBehovLøser(PdlPersondataOppslag(Config.properties), this)
            SøknadsdataBehovLøser(safClient, this)
            MinsteinntektVurderingLøser(
                regelApiClient = regelApiClient,
                repository = minsteinntektVurderingRepository,
                rapidsConnection = this
            )
            ArenaBehovLøser(arenaApiClient, this)
            OpprettGosysOppgaveLøser(gosysProxyClient, this)

            // Eksterne behovløsere
            SøknadFaktaQuizLøser(PostgresSøknadQuizOppslag(PostgresDataSourceBuilder.dataSource), this)
        }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        PostgresDataSourceBuilder.runMigration()
        søknadsDataVaktmester.fixManglendeSøknadsData()
        logg.info { "Starter dp-mottak" }
    }
}
