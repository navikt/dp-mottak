package no.nav.dagpenger.mottak

import java.util.UUID

sealed class Fagsystem(val fagsystemType: FagsystemType) {
    enum class FagsystemType {
        DAGPENGER,
        ARENA,
    }

    data class Dagpenger(val sakId: UUID) : Fagsystem(FagsystemType.DAGPENGER) {
        override fun toString(): String {
            return "Dagpenger(sakId=$sakId)"
        }
    }

    object Arena : Fagsystem(FagsystemType.ARENA) {
        override fun toString(): String {
            return "Arena"
        }
    }
}
