## Innløpet – flyt for brevkode GENERELL_INNSENDING
Kategorisert som: `Generell`, Behandlende enhet: `4450`
```mermaid
   stateDiagram
   [*]-->MottattType
   	MottattType --> AvventerJournalpostType
	AvventerJournalpostType --> AvventerPersondataType
	AvventerPersondataType --> KategoriseringType
	KategoriseringType --> AvventerSøknadsdataType
	AvventerSøknadsdataType --> AventerArenaOppgaveType
	AventerArenaOppgaveType --> AvventerFerdigstillJournalpostType
	AvventerFerdigstillJournalpostType --> InnsendingFerdigstiltType
   InnsendingFerdigstiltType--> [*]    
```