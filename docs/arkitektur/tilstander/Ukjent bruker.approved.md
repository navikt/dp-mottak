## Innløpet – flyt for brevkode Ukjent bruker
Kategorisert som: `Ettersending`, Behandlende enhet: `4450`
```mermaid
   stateDiagram
   [*]-->MottattType
   	MottattType --> AvventerJournalpostType
	AvventerJournalpostType --> AvventerPersondataType
	AvventerPersondataType --> UkjentBrukerType
	UkjentBrukerType --> InnsendingFerdigstiltType
   InnsendingFerdigstiltType--> [*]    
```