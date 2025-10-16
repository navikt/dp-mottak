package no.nav.dagpenger.mottak

import java.util.UUID

sealed class Fagsystem(val fagsystemType: FagsystemType) {
    enum class FagsystemType {
        DAGPENGER,
        ARENA,
    }

    data class Dagpenger(val sakId: UUID) : Fagsystem(FagsystemType.DAGPENGER)

    object Arena : Fagsystem(FagsystemType.ARENA)
}
