CREATE TABLE IF NOT EXISTS sak_v1
(
    innsending_id        BIGINT PRIMARY KEY REFERENCES innsending_v1 (id),
    sak_id               UUID   NOT NULL,
    ny_journalpost_id    BIGINT NULL,
    endret_tidspunkt     TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp),
    registrert_tidspunkt TIMESTAMP WITHOUT TIME ZONE DEFAULT timezone('Europe/Oslo'::text, current_timestamp)
);

CREATE INDEX IF NOT EXISTS sak_sak_id_index ON sak_v1 (sak_id);

CREATE OR REPLACE FUNCTION oppdater_endret_tidspunkt()
    RETURNS TRIGGER AS
$$
BEGIN
    NEW.endret_tidspunkt = timezone('Europe/Oslo'::text, current_timestamp);
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE OR REPLACE TRIGGER oppdater_endret_tidspunkt
    BEFORE UPDATE
    ON sak_v1
    FOR EACH ROW
EXECUTE FUNCTION oppdater_endret_tidspunkt();

