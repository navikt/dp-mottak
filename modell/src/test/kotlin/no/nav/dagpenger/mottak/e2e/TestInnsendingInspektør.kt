package no.nav.dagpenger.mottak.e2e

import com.spun.util.persistence.Loader
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingVisitor
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.NamerWrapper
import java.nio.file.Paths

class TestInnsendingInspektør(innsending: Innsending) : InnsendingVisitor {
    lateinit var gjeldendetilstand: InnsendingTilstandType
    internal lateinit var innsendingLogg: Aktivitetslogg

    init {
        innsending.accept(this)
    }

    override fun visitTilstand(tilstandType: Innsending.Tilstand) {
        gjeldendetilstand = tilstandType.type
    }

    override fun visitInnsendingAktivitetslogg(aktivitetslogg: Aktivitetslogg) {
        innsendingLogg = aktivitetslogg
    }
}

class PlantUmlObservatør() : InnsendingObserver {
    private val tilstander = mutableListOf<Pair<String, String>>()
    private val innsendingdetaljer = mutableListOf<String>()

    private companion object {
        val path = "${
            Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")
        }/docs/arkitektur/"
        val options =
            Options()
                .forFile()
                .withExtension(".md")
    }

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        tilstander.add(event.forrigeTilstand.name to event.gjeldendeTilstand.name)
    }

    override fun innsendingFerdigstilt(event: InnsendingObserver.InnsendingEvent) {
        innsendingdetaljer.add("Kategorisert som: `${event.type}`")
        innsendingdetaljer.add("Behandlende enhet: `${event.behandlendeEnhet}`")
    }

    fun toMermaidMd(brevkode: String) =
        """
        |## Innløpet – flyt for brevkode $brevkode
        |${innsendingdetaljer.joinToString()}
        |```mermaid
        |   stateDiagram
        |   [*]-->${tilstander.førsteTilstand()}
        |   ${tilstander.joinToString(separator = "\n") {
            "\t" + it.first + " --> " + it.second
        }
        }
        |   ${tilstander.sisteTilstand()}--> [*]    
        |```
        """.trimMargin()

    fun verify(brevkode: String) {
        Approvals.namerCreater = Loader { NamerWrapper({ "tilstander/$brevkode" }, { path }) }
        Approvals
            .verify(
                toMermaidMd(brevkode),
                options,
            )
    }
}

private fun MutableList<Pair<String, String>>.førsteTilstand(): String = this.first().first

private fun MutableList<Pair<String, String>>.sisteTilstand(): String = this.last().second
