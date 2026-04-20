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
- **OrkestratorSøknadFormat** — orkestrator-format med `versjon_namn: "OrkestratorSoknad"`
- **NullSøknadData** — fallback når ingen format matcher

Prioritet i `rutingOppslag()`: Brukerdialog → Quiz → Orkestrator → Null

#### Formatdeteksjon

```kotlin
fun rutingOppslag(data: JsonNode): RutingOppslag = when {
    data.path("verdi").isObject      -> BrukerdialogSøknadFormat(data)   // har verdi-wrapper
    data["versjon_navn"] == "Dagpenger"       -> QuizSøknadFormat(data)
    data["versjon_navn"] == "OrkestratorSoknad" -> OrkestratorSøknadFormat(data)
    else                                        -> NullSøknadData(data)
}
```

Denne logikken brukes **både** ved mottak fra Kafka og ved rehydrering fra DB.

#### `verdi`-wrapper og `data()` vs `eventData()`

BrukerdialogSøknadFormat wrapper all søknadsdata i et `verdi`-objekt:

```json
{
  "verdi": { "søknad_uuid": "...", "eøsArbeidsforhold": true, ... },
  "gjelderFra": "2026-04-17"
}
```

De eldre formatene (Quiz, Orkestrator) har **flat** struktur uten wrapper.

Denne forskjellen krever to ulike metoder på `SøknadOppslag`:

| Metode | Hva den returnerer | Brukes til |
|---|---|---|
| `data()` | Fullt objekt inkl. wrapper | Lagring i `soknad_v1` og rehydrering |
| `eventData()` | Indre objekt (uten wrapper) | Kafka-events (`innsending_ferdigstilt`, `innsending_mottatt`) |

Default-implementasjonen er `eventData() = data()`, så kun `BrukerdialogSøknadFormat` overstyrer:

```kotlin
// BrukerdialogSøknadFormat
override fun data(): JsonNode = data           // { "verdi": {...}, "gjelderFra": "..." }
override fun eventData(): JsonNode = verdi     // { "søknad_uuid": "...", ... }

// Quiz/Orkestrator/Null — bruker default
// eventData() = data()  (flat struktur, ingen wrapper)
```

**Bakgrunn:** Skillet ble innført etter en produksjonsfeil (april 2026) der `data()` returnerte kun indre objekt → DB-lagring mistet wrapper → rehydrering feilet → `NullSøknadData` → all ruting falt tilbake til 4450. Se `docs/APPLIKASJONSDOKUMENTASJON.md` for detaljer.

#### DB-oppslag med COALESCE

Fordi `soknad_v1` inneholder **begge** formater (gammelt uten wrapper, nytt med wrapper), må SQL-oppslag bruke COALESCE:

```sql
COALESCE(
    sokn.data -> 'verdi' ->> 'søknad_uuid',   -- nytt format (BrukerdialogSøknadFormat)
    sokn.data ->> 'søknad_uuid'                -- gammelt format (Quiz/Orkestrator)
) = :soknad_id
```

Nytt format sjekkes først. Hvis `verdi`-nøkkelen ikke finnes, returneres `null` og COALESCE faller til det flate oppslaget. Dette brukes i `InnsendingMetadataPostgresRepository` for `hentArenaOppgaver` og `hentDagpengerJournalpostIder`.

**Viktig:** `soknad_v1` bruker `INSERT ... ON CONFLICT DO NOTHING` — data oppdateres aldri. Gamle rader forblir i gammelt format. COALESCE-mønsteret må opprettholdes så lenge det finnes data i begge formater.

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
