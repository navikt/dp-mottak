package no.nav.dagpenger.mottak.behov.eksterne

import no.nav.helse.rapids_rivers.JsonMessage
import no.nav.helse.rapids_rivers.MessageContext
import no.nav.helse.rapids_rivers.RapidsConnection
import no.nav.helse.rapids_rivers.River

internal class SøknadFaktaLøser(private val søknadsOppslag: SøknadsOppslag, rapidsConnection: RapidsConnection) :
    River.PacketListener {
    private val løserBehov = listOf(
        "ØnskerDagpengerFraDato",
        "Søknadstidspunkt",
        "Verneplikt",
        "FangstOgFiske",
        "SisteDagMedArbeidsplikt",
        "SisteDagMedLønn",
        "Lærling",
        "EØSArbeid",
        "Rettighetstype"
    )

    init {
        River(rapidsConnection).apply {
            validate { it.demandValue("@event_name", "faktum_svar") }
            validate { it.demandAllOrAny("@behov", løserBehov) }
            validate { it.requireKey("InnsendtSøknadsId") }
        }.register(this)
    }

    override fun onPacket(packet: JsonMessage, context: MessageContext) {
        val søknad = søknadsOppslag.hentSøknad(packet["InnsendtSøknadsId"]["url"].asText())
        packet["@løsning"] = packet["@behov"].map { it.asText() }.filter { it in løserBehov }.map { behov ->
            behov to when (behov) {
                "ØnskerDagpengerFraDato" ->
                    søknad.ønskerDagpengerFraDato()
                "Søknadstidspunkt" -> søknad.søknadstidspunkt()
                "Verneplikt" -> søknad.verneplikt()
                "FangstOgFiske" -> søknad.fangstOgFisk()
                "EØSArbeid" -> søknad.jobbetIeøs()
                "SisteDagMedArbeidsplikt" -> søknad.sisteDagMedArbeidsplikt()
                "SisteDagMedLønn" -> søknad.sisteDagMedLønn()
                "Lærling" -> søknad.lærling()
                "Rettighetstype" -> søknad.rettighetstype().name

                else -> throw IllegalArgumentException("Ukjent behov $behov")
            }
        }.toMap()
        context.publish(packet.toJson())
    }
}
