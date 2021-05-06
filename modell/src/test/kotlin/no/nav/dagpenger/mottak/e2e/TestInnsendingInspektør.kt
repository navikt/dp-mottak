package no.nav.dagpenger.mottak.e2e

import no.nav.dagpenger.mottak.Aktivitetslogg
import no.nav.dagpenger.mottak.Innsending
import no.nav.dagpenger.mottak.InnsendingObserver
import no.nav.dagpenger.mottak.InnsendingTilstandType
import no.nav.dagpenger.mottak.InnsendingVisitor

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

class PlantUmlObservatør():InnsendingObserver{
    internal val tilstander = mutableListOf<String>()

    override fun tilstandEndret(event: InnsendingObserver.InnsendingEndretTilstandEvent){
        tilstander.add("${event.forrigeTilstand} --> ${event.gjeldendeTilstand.name}")
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
}

private fun MutableList<String>.førsteTilstand(): String = this.first().substringBefore("--> ")
private fun MutableList<String>.sisteTilstand(): String = this.last().substringAfter("--> ")

