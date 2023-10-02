package no.nav.dagpenger.mottak.e2e

import com.spun.util.persistence.Loader
import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingVisitor
import org.approvaltests.Approvals
import org.approvaltests.core.Options
import org.approvaltests.namer.ApprovalNamer
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
    private val tilstander = mutableListOf<String>()

    private companion object {
        val path = "${
            Paths.get("").toAbsolutePath().toString().substringBeforeLast("/")
        }/docs/arkitektur/"
        val options =
            Options()
                .forFile()
                .withExtension(".puml")
    }

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent) {
        tilstander.add("${event.forrigeTilstand} --> ${event.gjeldendeTilstand.name}")
    }

    fun reset() {
        tilstander.clear()
    }

    fun toPlantUml(brevkode: String): String =
        """
          |@startuml
          |title 
          |Innløpet – flyt for brevkode $brevkode
          |end title           
          |[*]-->${tilstander.førsteTilstand()}
          |${tilstander.joinToString("\n")}
          |${tilstander.sisteTilstand()}--> [*]
          |@enduml
        """.trimMargin()

    fun verify(brevkode: String) {
        Approvals.namerCreater = Loader<ApprovalNamer> { NamerWrapper({ "tilstander/$brevkode" }, { path }) }
        Approvals
            .verify(
                toPlantUml(brevkode),
                options,
            )
    }
}

private fun MutableList<String>.førsteTilstand(): String = this.first().substringBefore("--> ")

private fun MutableList<String>.sisteTilstand(): String = this.last().substringAfter("--> ")
