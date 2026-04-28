# Post Mortem: Ettersendinger fikk feil brevkode → manuelle journalføringsoppgaver

## Oppsummering

Ettersendinger fra dp-brukerdialog-frontend (ny søknadsflyt) fikk skjemakode direkte fra frontend-payloaden (f.eks. «O2», «T6», «T8») istedenfor riktig NAVe-prefiks. dp-mottak kategoriserte disse som `UkjentSkjemaKode`, som alltid går til Gosys som manuell journalføringsoppgave på benk 4450.

- **Alvorlighetsgrad:** Middels — ingen datatap, men betydelig merarbeid for saksbehandlere
- **Varighet:** Ca. 3 måneder (fra ettersendingsfunksjonen ble tatt i bruk i prod ~februar 2026 til fix 27. april 2026)
- **Berørt system:** dp-soknad-orkestrator → dp-behov-journalforing → dp-mottak

## Bakgrunn

Teamet har utviklet en ny søknadsflyt via dp-brukerdialog-frontend / dp-soknad-orkestrator som erstatter den eldre Quiz-baserte flyten. Som del av dette ble det lagt til støtte for ettersending av dokumenter (PR #299, 2. februar 2026). Når bruker ettersender dokumenter (arbeidsavtale, permitteringsvarsel, etc.), oppretter dp-soknad-orkestrator en journalpost via dp-behov-journalforing.

Journalpostens hoveddokument-brevkode er avgjørende for hvordan dp-mottak kategoriserer og ruter innsendingen. dp-mottak forventer at ettersendinger har brevkode med «NAVe»-prefiks (f.eks. «NAVe 04-01.03») for å kategorisere dem som `Ettersending`, som automatisk journalføres via Arena.

## Beskriv feilen

Symptomene var økt volum av manuelle journalføringsoppgaver — spesielt arbeidsavtaler (O2) — på 4450-benken. Disse oppgavene ble tidligere automatisk journalført.

### Teknisk rotårsak

I `SeksjonService.kt` brukte `opprettDokumenterFraDokumentasjonskravEttersending()` skjemakoden fra frontend-payloaden direkte (f.eks. «T8», «O2»). Denne koden ble satt som brevkode på hoveddokumentet i journalposten.

Flyten:

```
1. Bruker ettersender dokument via dp-brukerdialog-frontend
2. dp-soknad-orkestrator oppretter MeldingOmEttersending med skjemakode="O2" (fra frontend)
3. dp-behov-journalforing oppretter journalpost med brevkode="O2"
4. dagpenger-joark-mottak plukker opp hendelsen (tema=DAG)
5. dp-mottak henter journalpost fra SAF → brevkode="O2"
6. dp-mottak: kategorisertJournalpost() → "O2" matcher ingen kjent kode → UkjentSkjemaKode
7. dp-saksbehandling: håndtert=false (ingen sak i nytt system)
8. UkjentSkjemaKode + Arena-path → AvventerGosysOppgave → manuell oppgave på 4450
```

Riktig flyt (etter fix):
```
1. dp-soknad-orkestrator: hoveddokument.skjemakode = SøknadService.finnSkjemaKode() (f.eks. "04-01.03")
2. dp-behov-journalforing legger på NAVe-prefiks → brevkode="NAVe 04-01.03"
3. dp-mottak: kategorisertJournalpost() → "NAVe 04-01.03" → Ettersending
4. Ettersending + Arena-path → VurderHenvendelse → automatisk journalføring
```

## Hvordan ble problemet oppdaget

Problemet ble rapportert av saksbehandlere som merket økt antall manuelle journalføringsoppgaver for arbeidsavtaler og lignende vedlegg på 4450-benken. De kontaktet teamet 28. april.

### Kunne vi oppdaget det tidligere?

Ja:
- **Manglende metrikker/alarmer:** Vi har ingen alarm for volumøkning av Gosys-oppgaver eller for at ettersendinger blir kategorisert som UkjentSkjemaKode
- **Manglende E2E-test:** Det fantes ingen integrasjonstest som verifiserte at ettersendinger fra ny søknadsflyt endte opp med riktig brevkode i journalposten
- **Manuell testing:** Ettersendingsfunksjonen ble testet manuelt, men verifiseringen stoppet ved at journalposten ble opprettet — ikke at dp-mottak kategoriserte den riktig

## Påvirkning

- **Saksbehandlere (4450):** Betydelig merarbeid. Alle ettersendinger fra ny søknadsflyt havnet som manuelle oppgaver
- **Sluttbrukere:** Forsinkede behandlingstider da vedleggene måtte håndteres manuelt
- **NAV:** Ineffektiv ressursbruk — oppgaver som skulle vært automatisert krevde manuell innsats
- **Omfang:** Alle brukere som ettersendte dokumenter via dp-brukerdialog-frontend var berørt. Antall berørte avhenger av hvor mange som brukte ny søknadsflyt med ettersendinger (begrenset utbredelse)

## Respons

- **28. april:** Saksbehandlere melder inn problemet
- **28. april:** Teamet starter analyse i dp-mottak — sporer flyten fra brevkode → UkjentSkjemaKode → Gosys
- **28. april:** Identifiserer at dp-mottak aldri har endret denne rutingen — UkjentSkjemaKode + Arena har alltid gått til Gosys
- **28. april:** Sporer opp rotårsaken til dp-soknad-orkestrator

### Hindringer

Undersøkelsen startet i dp-mottak og tok tid fordi det var uklart om rutingen hadde endret seg der. Det tok flere steg å følge tråden tilbake til dp-soknad-orkestrator som kilden.

## Gjenoppretting

**Fix:** [PR #376](https://github.com/navikt/dp-soknad-orkestrator/pull/376) i dp-soknad-orkestrator (merget 27. april 2026):

1. **SeksjonService:** Hoveddokumentet ved ettersending får nå skjemakode fra `SøknadService.finnSkjemaKode()` basert på søkerens svar, istedenfor koden fra frontend-payloaden. dp-behov-journalforing legger på NAVe-prefiks.

2. **SafKlient:** Returnerer null uten å hente dokumentinnhold dersom brevkoden starter med «NAVe». Ettersendinger inneholder PDF (ikke JSON) og ville krasjet ved parseforsøk.

### Kunne vi gjort det raskere?

- Bedre observerbarhet (metrikker for UkjentSkjemaKode-volum, alarm på Gosys-oppgaveøkning) ville gitt tidlig varsling
- End-to-end-tester som verifiserer hele flyten fra ettersending til dp-mottak-kategorisering ville fanget dette før produksjon

## Tidslinje

| Dato | Hendelse |
|------|----------|
| 2. feb 2026 | Ettersending-endepunkt lagt til i dp-soknad-orkestrator (PR #299) |
| Feb–april 2026 | Ettersendinger fra ny søknadsflyt kategoriseres feil som UkjentSkjemaKode → Gosys |
| 27. april 2026 | Fix merget: PR #376 i dp-soknad-orkestrator |
| 28. april 2026 | Saksbehandlere melder inn økt antall manuelle oppgaver |
| 28. april 2026 | Teamet analyserer og bekrefter at fixen allerede er deployet |

## Rotårsak

dp-soknad-orkestrator brukte skjemakode fra frontend-payloaden (f.eks. «O2», «T8») som hoveddokument-brevkode ved ettersending. dp-behov-journalforing la ikke på NAVe-prefiks fordi den mottatte koden ikke var en standard NAV-skjemakode. dp-mottak gjenkjente ikke disse kodene og kategoriserte dem som `UkjentSkjemaKode`, som på Arena-pathen alltid går til Gosys for manuell journalføring.

## Læring

### Hva gikk bra
- Fixen var allerede merget (27. april) da meldingen fra saksbehandlerne kom (28. april) — teamet hadde oppdaget feilen selv
- PR #376 inkluderte gode tester (SafKlientTest, SeksjonServiceTest)
- Commit-meldingen dokumenterte problemet tydelig

### Hva kunne vi gjort bedre
- **Manglende tverrfaglig testing:** Ingen test verifiserte at brevkoden ble korrekt gjennom hele kjeden (orkestrator → journalforing → mottak)
- **Ingen varsling:** Mangler alarm for unormalt volum av UkjentSkjemaKode eller Gosys-oppgaver
- **Lang levetid:** Feilen eksisterte i ~3 måneder før den ble oppdaget og fikset
- **Dokumentasjon:** Kontrakten mellom dp-soknad-orkestrator og dp-mottak (at brevkode MÅ ha NAVe-prefiks for ettersendinger) var ikke eksplisitt dokumentert

## Tiltak

| # | Tiltak | Prioritet | Risiko uten tiltak |
|---|--------|-----------|--------------------|
| 1 | ✅ Fix brevkode i dp-soknad-orkestrator (PR #376) | Høy — utført | Løst |
| 2 | Verifiser at eksisterende feilopprettede Gosys-oppgaver er håndtert | Høy | Saksbehandlere kan ha oppgaver som allerede er utdaterte |
| 3 | Alarm: økning i UkjentSkjemaKode-volum i dp-mottak | Middels | Lignende feil kan gå uoppdaget i måneder |
| 4 | Alarm: økning i Gosys-oppgaver fra dp-mottak | Middels | Samme som over |
| 5 | Dokumenter kontrakten mellom orkestrator og dp-mottak (NAVe-prefiks krav) | Middels | Risiko for gjentakelse ved nye endringer |
| 6 | Integrasjonstest som verifiserer hele ettersending-flyten | Lav | Regresjonsrisiko ved fremtidige endringer |

---

*Målet er læring og forbedring. Fokus er på hendelsen — ikke person.*
