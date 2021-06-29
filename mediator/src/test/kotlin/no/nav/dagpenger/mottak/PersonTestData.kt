package no.nav.dagpenger.mottak

import no.bekk.bekkopen.person.FodselsnummerCalculator
import java.time.LocalDate
import java.time.ZoneId
import java.util.Date

object PersonTestData {
    private val dato = Date.from(
        LocalDate.now().minusYears(20).atStartOfDay(
            ZoneId.systemDefault()
        ).toInstant()
    )
    val GENERERT_FØDSELSNUMMER by lazy { FodselsnummerCalculator.getFodselsnummerForDate(dato).toString() }
    val GENERERT_DNUMMER by lazy { FodselsnummerCalculator.getManyDNumberFodselsnummerForDate(dato).first().toString() }
}
