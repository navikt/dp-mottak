## Innløpet – flyt for brevkode Ferdigstilte journalposter
Kategorisert som: `UkjentSkjemaKode`, Behandlende enhet: `4450`
```mermaid
   stateDiagram
   [*]-->MottattType
   	MottattType --> AvventerJournalpostType
	AvventerJournalpostType --> AvventerPersondataType
	AvventerPersondataType --> KategoriseringType
	KategoriseringType --> AvventerGosysType
	AvventerGosysType --> InnsendingFerdigstiltType
   InnsendingFerdigstiltType--> [*]    
```