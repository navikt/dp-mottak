CREATE INDEX IF NOT EXISTS soknad_v1_soknad_uuid_idx
    ON soknad_v1 (
        COALESCE(
            data -> 'verdi' ->> 'søknad_uuid',
            data ->> 'søknad_uuid'
        )
    );
