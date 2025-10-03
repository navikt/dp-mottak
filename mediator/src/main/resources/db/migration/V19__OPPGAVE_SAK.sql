CREATE TABLE IF NOT EXISTS oppgave_sak_v1
(
    id         BIGINT PRIMARY KEY REFERENCES innsending_v1,
    fagsak_id  UUID NOT NULL,
    oppgave_id UUID NOT NULL
);
