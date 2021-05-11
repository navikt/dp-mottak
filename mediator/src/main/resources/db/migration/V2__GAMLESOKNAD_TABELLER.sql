CREATE TABLE IF NOT EXISTS v1_soknad_journalpost_mapping
(
    soknads_id     VARCHAR(20) NOT NULL,
    journalpost_id VARCHAR(12) NOT NULL,
    PRIMARY KEY (soknads_id, journalpost_id)
);

CREATE TABLE IF NOT EXISTS v2_soknad
(
    fnr        CHAR(11)                                                      NOT NULL,
    soknads_id VARCHAR(20)                                                   NOT Null,
    data       JSONB                                                         NOT NULL,
    created    TIMESTAMP WITH TIME ZONE DEFAULT timezone('utc'::text, now()) NOT NULL,
    processed  BOOLEAN                  DEFAULT false,
    PRIMARY KEY (fnr, soknads_id)
);
