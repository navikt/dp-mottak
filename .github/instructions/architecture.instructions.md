---
applyTo: "**/*.kt"
---

# dp-mottak Arkitektur

Arkitekturbeslutninger og domenemodell for dp-mottak — hendelsesdrevet mottak av dagpenger-dokumenter.

## Hva dp-mottak gjør

dp-mottak lytter på joark-hendelser (nye journalposter) og driver dem gjennom en tilstandsmaskin:

```
JoarkHendelse → Journalpost → Persondata → Kategorisering → Søknadsdata → HåndterInnsending → ...
```

Hver tilstand publiserer et **behov** på Kafka, og en **behovløser** (enten intern eller ekstern) svarer med en løsning.

## Behov og hvem som løser dem

| Behov | Løses av | Merknad |
|---|---|---|
| Journalpost | dp-mottak (SafClient, GraphQL) | Henter journalpostdata fra SAF |
| Persondata | dp-mottak (PdlPersondataOppslag) | Henter person fra PDL |
| Søknadsdata | **dp-soknad-orkestrator** | Flyttet ut av dp-mottak (april 2026) |
| HåndterInnsending | **dp-saksbehandling** | Avgjør om dp-saksbehandling eller Arena håndterer |
| OppdaterJournalpost | dp-mottak (DokarkivClient) | Oppdaterer journalpost i Joark |
| OpprettStartVedtakOppgave | dp-mottak (ArenaOppslag via dp-proxy) | Arena-oppgave |
| OpprettGosysoppgave | dp-mottak (GosysOppgaveClient) | Gosys-oppgave |
| FerdigstillJournalpost | dp-mottak (DokarkivClient) | Ferdigstiller journalpost |

### Søknadsdata — løses eksternt

`SøknadsdataBehovLøser` er **fjernet** fra dp-mottak. Behovet løses nå av `dp-soknad-orkestrator`.

dp-mottak publiserer behovet med `ident` og `dokumentInfoId`, og orkestratoren svarer med søknadsdata i ett av disse formatene:

- **BrukerdialogSøknadFormat** — nytt format fra innbyggerflate, wrappet i `{ "verdi": {...}, "gjelderFra": "..." }`
- **QuizSøknadFormat** — gammelt quiz-format med `versjon_navn: "Dagpenger"` og `seksjoner`
- **OrkestratorSøknadFormat** — orkestrator-format med `versjon_navn: "OrkestratorSoknad"`
- **NullSøknadData** — fallback når ingen format matcher

Prioritet i `rutingOppslag()`: Brukerdialog → Quiz → Orkestrator → Null

### HåndterInnsending — løses eksternt

dp-saksbehandling løser dette behovet. Detaljer inkluderer `kategori`, `fødselsnummer`, `journalpostId`, `registrertDato`, `skjemaKode`, og **valgfritt** `søknadId` (kun når søknadsdata inneholder `søknad_uuid`).

## SAF-integrasjon

SafClient brukes **kun** for GraphQL journalpost-oppslag (`JournalpostArkiv`).

REST-endepunktet for søknadsdata (hentSøknadsData) er **fjernet** — all søknadsdata hentes nå via dp-soknad-orkestrator over Kafka.

Klasser som er fjernet:
- `SøknadsdataBehovLøser` — deaktivert, så fjernet
- `SøknadsArkiv` — interface for REST søknadsdata
- `SøknadsDataVaktmester` — engangsjobb for reparasjon, aldri registrert

## Viktige mønstre

### Jackson: textValue() vs asText()

Bruk `textValue()` for nullable strenger fra JSON — **ikke** `asText()`:

```kotlin
// ✅ Riktig — returnerer null for JSON null
node["felt"]?.textValue()

// ❌ Feil — returnerer strengen "null" for JSON null
node["felt"]?.asText()
```

Dette forårsaket en produksjonsfeil der `søknadId: "null"` ble sendt på Kafka.

### Behov-detaljer med valgfrie felter

```kotlin
val detaljer = buildMap<String, Any> {
    this["påkrevdFelt"] = verdi
    valgfriFelt?.let { this["valgfriFelt"] = it }  // Kun med når ikke null
}.toMap()
```
