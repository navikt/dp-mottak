## Innløpet – flyt for brevkode JournalpostStatus annen enn MOTTATT

```mermaid
   stateDiagram
   [*]-->MottattType
   	MottattType --> AvventerJournalpostType
	AvventerJournalpostType --> AlleredeBehandletType
   AlleredeBehandletType--> [*]    
```