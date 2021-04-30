CREATE TABLE IF NOT EXISTS innsending_v1
(
    id            BIGSERIAL PRIMARY KEY,
    journalpostId BIGINT UNIQUE,
    tilstand      VARCHAR(64)              NOT NULL,
    opprettet     TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);

CREATE TABLE IF NOT EXISTS arenasak_v1
(
    id        BIGINT PRIMARY KEY REFERENCES innsending_v1,
    fagsakId  VARCHAR(64),
    oppgaveId VARCHAR(64) NOT NULL
);

CREATE TABLE IF NOT EXISTS innsending_oppfyller_minsteinntekt_v1
(
    id    BIGINT PRIMARY KEY REFERENCES innsending_v1,
    verdi BOOLEAN NOT NULL
);


CREATE TABLE IF NOT EXISTS innsending_eksisterende_arena_saker_v1
(
    id    BIGINT PRIMARY KEY REFERENCES innsending_v1,
    verdi BOOLEAN NOT NULL
);


CREATE TABLE IF NOT EXISTS journalpost_v1
(
    id              BIGINT PRIMARY KEY REFERENCES innsending_v1,
    status          VARCHAR(128)                NOT NULL,
    brukerId        VARCHAR(20)                 NULL,
    brukerType      VARCHAR(20)                 NULL,
    behandlingstema VARCHAR(20)                 NULL,
    registrertDato  TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS journalpost_dokumenter_v1
(
    id             BIGINT REFERENCES journalpost_v1,
    tittel         VARCHAR(255) NOT NULL,
    dokumentInfoId BIGINT      NOT NULL,
    brevkode       VARCHAR(20) NOT NULL,
    PRIMARY KEY(dokumentInfoId,id,brevkode)
);

CREATE TABLE IF NOT EXISTS soknad_v1
(
    id   BIGINT PRIMARY KEY REFERENCES innsending_v1,
    data JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS person_v1
(
    id            BIGSERIAL PRIMARY KEY,
    fødselsnummer VARCHAR(11) UNIQUE NOT NULL,
    aktørId       VARCHAR(32) UNIQUE NOT NULL
);

CREATE TABLE IF NOT EXISTS person_innsending_v1
(
    id               BIGINT PRIMARY KEY REFERENCES innsending_v1,
    personId         BIGSERIAL REFERENCES person_v1 (id),
    norsktilknytning BOOLEAN NOT NULL,
    diskresjonskode  BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS aktivitetslogg_v1
(
    id   BIGINT PRIMARY KEY REFERENCES innsending_v1,
    data JSONB NOT NULL
);