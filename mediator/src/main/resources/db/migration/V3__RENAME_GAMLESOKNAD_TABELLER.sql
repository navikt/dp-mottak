ALTER TABLE IF EXISTS v1_soknad_journalpost_mapping
    RENAME TO gammel_journalpost_mapping;

ALTER TABLE IF EXISTS v2_soknad
    RENAME TO gammel_soknad;
