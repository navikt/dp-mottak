package no.nav.dagpenger.mottak.meldinger.søknadsdata

import com.fasterxml.jackson.databind.JsonNode
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.AvsluttetArbeidsforhold
import no.nav.dagpenger.mottak.Hendelse
import no.nav.dagpenger.mottak.RutingOppslag
import no.nav.dagpenger.mottak.SøknadVisitor

class Søknadsdata(
    aktivitetslogg: Aktivitetslogg,
    private val journalpostId: String,
    private val data: JsonNode,
) : Hendelse(aktivitetslogg) {
    override fun journalpostId(): String = journalpostId

    fun søknad(): RutingOppslag {
        return rutingOppslagV2(data)
    }
}

internal fun rutingOppslagV2(data: JsonNode): RutingOppslag {
    val versjonNavn = data["versjon_navn"]?.asText()
    when (versjonNavn) {
        "Dagpenger" -> return QuizSøknadFormat(data)
        "Dagpenger_v2" -> return OrkestratorSøknadFormat(data)
        else -> return NullSøknadData(data)
    }
}

class NullSøknadData(private val data: JsonNode) : RutingOppslag {
    override fun data() = data

    override fun accept(visitor: SøknadVisitor) {
        visitor.visitSøknad(this)
    }

    override fun eøsBostedsland() = false

    override fun eøsArbeidsforhold() = false

    override fun avtjentVerneplikt() = false

    override fun avsluttetArbeidsforhold() = emptyList<AvsluttetArbeidsforhold>()

    override fun harBarn() = false

    override fun harAndreYtelser() = false

    override fun permittertFraFiskeForedling() = false

    override fun avsluttetArbeidsforholdFraKonkurs() = false

    override fun permittert() = false
}
