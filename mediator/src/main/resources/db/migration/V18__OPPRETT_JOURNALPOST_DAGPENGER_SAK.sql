CREATE TABLE IF NOT EXISTS journalpost_dagpenger_sak_v1
(
    innsending_id  BIGINT PRIMARY KEY REFERENCES innsending_v1 (id),
    fagsak_id      UUID   NOT NULL,
    journalpost_id BIGINT NOT NULL
);
CREATE INDEX journalpost_dagpenger_sak_v1_fagsak_id ON journalpost_dagpenger_sak_v1 (fagsak_id);
