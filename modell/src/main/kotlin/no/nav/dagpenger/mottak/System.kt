package no.nav.dagpenger.mottak

import java.util.UUID

sealed class System(val fagsystem: Fagsystem) {
    enum class Fagsystem {
        DAGPENGER,
        ARENA,
    }

    data class Dagpenger(val sakId: UUID) : System(Fagsystem.DAGPENGER)

    object Arena : System(Fagsystem.ARENA)
}
