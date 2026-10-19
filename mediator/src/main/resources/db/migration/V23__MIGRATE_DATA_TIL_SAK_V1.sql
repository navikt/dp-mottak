INSERT INTO sak_v1 (innsending_id, sak_id, ny_journalpost_id)
SELECT innsending_id, fagsak_id, journalpost_id FROM journalpost_dagpenger_sak_v1;

INSERT INTO sak_v1 (innsending_id, sak_id)
SELECT id, fagsak_id FROM oppgave_sak_v1
ON CONFLICT (innsending_id) DO NOTHING;