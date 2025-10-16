package no.nav.dagpenger.mottak

import com.github.navikt.tbd_libs.naisful.naisApp
import com.github.navikt.tbd_libs.rapids_and_rivers_api.RapidsConnection
import io.github.oshai.kotlinlogging.KotlinLogging
import io.micrometer.core.instrument.Clock
import io.micrometer.prometheusmetrics.PrometheusConfig
import io.micrometer.prometheusmetrics.PrometheusMeterRegistry
import io.prometheus.metrics.model.registry.PrometheusRegistry
import no.nav.dagpenger.mottak.Config.dokarkivTokenProvider
import no.nav.dagpenger.mottak.Config.objectMapper
import no.nav.dagpenger.mottak.api.innsendingApi
import no.nav.dagpenger.mottak.api.installPlugins
import no.nav.dagpenger.mottak.api.journalpostRoute
import no.nav.dagpenger.mottak.api.statusPages
import no.nav.dagpenger.mottak.behov.journalpost.FerdigstillJournalpostBehovLøser
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApiClient
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostBehovLøser
import no.nav.dagpenger.mottak.behov.journalpost.OppdaterJournalpostBehovLøser
import no.nav.dagpenger.mottak.behov.journalpost.SafClient
import no.nav.dagpenger.mottak.behov.journalpost.SøknadsdataBehovLøser
import no.nav.dagpenger.mottak.behov.person.PdlPersondataOppslag
import no.nav.dagpenger.mottak.behov.person.PersondataBehovLøser
import no.nav.dagpenger.mottak.behov.person.SkjermingOppslag
import no.nav.dagpenger.mottak.behov.person.createPersonOppslag
import no.nav.dagpenger.mottak.behov.saksbehandling.DagpengerOppgaveBehovLøser
import no.nav.dagpenger.mottak.behov.saksbehandling.FagystemBehovLøser
import no.nav.dagpenger.mottak.behov.saksbehandling.SaksbehandlingHttpKlient
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaApiClient
import no.nav.dagpenger.mottak.behov.saksbehandling.arena.ArenaBehovLøser
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.GosysClient
import no.nav.dagpenger.mottak.behov.saksbehandling.gosys.OpprettGosysOppgaveLøser
import no.nav.dagpenger.mottak.behov.saksbehandling.ruting.SakseierBasertRuting
import no.nav.dagpenger.mottak.db.InnsendingMetadataPostgresRepository
import no.nav.dagpenger.mottak.db.InnsendingMetadataRepository
import no.nav.dagpenger.mottak.db.InnsendingPostgresRepository
import no.nav.dagpenger.mottak.db.PostgresDataSourceBuilder
import no.nav.dagpenger.mottak.observers.FerdigstiltEttersendingObserver
import no.nav.dagpenger.mottak.observers.FerdigstiltInnsendingObserver
import no.nav.dagpenger.mottak.observers.InnsendingProbe
import no.nav.dagpenger.mottak.observers.MetrikkObserver
import no.nav.dagpenger.mottak.tjenester.MottakMediator
import no.nav.dagpenger.mottak.tjenester.VedtakFattetMottak
import no.nav.helse.rapids_rivers.RapidApplication
import org.slf4j.LoggerFactory

private val logg = KotlinLogging.logger {}

internal class ApplicationBuilder(
    env: Map<String, String>,
) : RapidsConnection.StatusListener {
    private val innsendingRepository = InnsendingPostgresRepository(PostgresDataSourceBuilder.dataSource)
    private val innsendingMetadataRepository: InnsendingMetadataRepository = InnsendingMetadataPostgresRepository(PostgresDataSourceBuilder.dataSource)
    private val safClient = SafClient(Config.properties)
    private val arenaApiClient = ArenaApiClient(Config.properties)
    private val journalpostApiClient = JournalpostApiClient(tokenProvider = Config.properties.dokarkivTokenProvider)
    private val gosysOppslag = GosysClient(Config.properties)
    private val ferdigstiltInnsendingObserver = FerdigstiltInnsendingObserver(Config.kafkaProducerProperties)
    private val saksbehandlingKlient =
        SaksbehandlingHttpKlient(
            dpSaksbehandlingBaseUrl = Config.dpSaksbehandlingBaseUrl,
            tokenProvider = Config.dpSaksbehandlingTokenProvider,
        )

    val sakseierBasertRuting = SakseierBasertRuting(saksbehandlingKlient)
    private val rapidsConnection =
        RapidApplication
            .create(env = env, builder = {
                withKtor { preStopHook, rapid ->
                    naisApp(
                        meterRegistry =
                            PrometheusMeterRegistry(
                                PrometheusConfig.DEFAULT,
                                PrometheusRegistry.defaultRegistry,
                                Clock.SYSTEM,
                            ),
                        objectMapper = objectMapper,
                        applicationLogger = LoggerFactory.getLogger("ApplicationLogger"),
                        callLogger = LoggerFactory.getLogger("CallLogger"),
                        aliveCheck = rapid::isReady,
                        readyCheck = rapid::isReady,
                        preStopHook = preStopHook::handlePreStopRequest,
                        statusPagesConfig = {
                            statusPages()
                        },
                    ) {
                        installPlugins {
                            journalpostRoute(innsendingMetadataRepository)
                            innsendingApi(innsendingRepository, ferdigstiltInnsendingObserver)
                        }
                    }
                }
            })
            .apply {
                val mediator =
                    InnsendingMediator(
                        innsendingRepository = innsendingRepository,
                        rapidsConnection = this,
                        observatører =
                            listOf(
                                ferdigstiltInnsendingObserver,
                                FerdigstiltEttersendingObserver(
                                    saksbehandlingKlient = saksbehandlingKlient,
                                    gosysClient = gosysOppslag,
                                ),
                                MetrikkObserver(),
                                InnsendingProbe,
                            ),
                    )
                // Behovmottakere
                MottakMediator(mediator, this)

                // Behovløsere
                JournalpostBehovLøser(safClient, this)
                OppdaterJournalpostBehovLøser(journalpostApiClient, this)
                FerdigstillJournalpostBehovLøser(journalpostApiClient, this)
                PersondataBehovLøser(
                    createPersonOppslag(PdlPersondataOppslag(Config.properties), SkjermingOppslag(Config.properties)),
                    this,
                )
                SøknadsdataBehovLøser(safClient, this)
                ArenaBehovLøser(arenaApiClient, this)
                OpprettGosysOppgaveLøser(gosysOppslag, this)
                VedtakFattetMottak(
                    rapidsConnection = this,
                    innsendingMetadataRepository = innsendingMetadataRepository,
                    journalpostDokarkiv = journalpostApiClient,
                )
                FagystemBehovLøser(
                    rapidsConnection = this,
                    oppgaveRuting =
                        SakseierBasertRuting(
                            saksbehandlingKlient = saksbehandlingKlient,
                        ),
                )
                DagpengerOppgaveBehovLøser(
                    saksbehandlingKlient = saksbehandlingKlient,
                    rapidsConnection = this,
                )
            }

    init {
        rapidsConnection.register(this)
    }

    fun start() = rapidsConnection.start()

    override fun onStartup(rapidsConnection: RapidsConnection) {
        PostgresDataSourceBuilder.runMigration()
        logg.info { "Starter dp-mottak" }
    }
}
