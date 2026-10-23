package no.nav.dagpenger.mottak.behov.journalpost

import io.kotest.matchers.shouldBe
import no.nav.dagpenger.mottak.Fagsystem.FagsystemType.ARENA
import no.nav.dagpenger.mottak.Fagsystem.FagsystemType.DAGPENGER
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.SaksType.FAGSAK
import no.nav.dagpenger.mottak.behov.journalpost.JournalpostApi.SaksType.GENERELL_SAK
import org.junit.jupiter.api.Test

class SakTest {
    @Test
    fun `Skal sette riktig fagsaksystem og sakstype`() {
        JournalpostApi
            .Sak(
                fagsakId = null,
                fagsystem = DAGPENGER,
            ).also {
                it.fagsaksystem shouldBe null
                it.saksType shouldBe GENERELL_SAK
            }

        JournalpostApi
            .Sak(
                fagsakId = null,
                fagsystem = ARENA,
            ).also {
                it.fagsaksystem shouldBe null
                it.saksType shouldBe GENERELL_SAK
            }

        JournalpostApi
            .Sak(
                fagsakId = "123",
                fagsystem = ARENA,
            ).also {
                it.fagsaksystem shouldBe "AO01"
                it.saksType shouldBe FAGSAK
            }
        JournalpostApi
            .Sak(
                fagsakId = "123",
                fagsystem = DAGPENGER,
            ).also {
                it.fagsaksystem shouldBe "DAGPENGER"
                it.saksType shouldBe FAGSAK
            }

        JournalpostApi
            .Sak(
                fagsakId = "123",
                fagsystem = null,
            ).also {
                it.fagsaksystem shouldBe null
                it.saksType shouldBe GENERELL_SAK
            }
    }
}
