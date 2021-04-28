CREATE TABLE IF NOT EXISTS innsending_v1
(
    id            BIGSERIAL,
    journalpostId BIGINT PRIMARY KEY,
    tilstand      VARCHAR(64)              NOT NULL,
    opprettet     TIMESTAMP WITH TIME ZONE NOT NULL default (now() at time zone 'utc')
);

CREATE TABLE IF NOT EXISTS arenasak_v1
(
    fagsakId      VARCHAR(64) primary key,
    oppgaveId     VARCHAR(64) NOT NULL,
    journalpostId BIGINT REFERENCES innsending_v1
);

CREATE TABLE IF NOT EXISTS innsending_oppfyller_minsteinntekt_v1
(
    journalpostId BIGINT REFERENCES innsending_v1,
    verdi         BOOLEAN NOT NULL
);


CREATE TABLE IF NOT EXISTS innsending_eksisterende_arena_saker_v1
(
    journalpostId BIGINT REFERENCES innsending_v1,
    verdi         BOOLEAN NOT NULL
);


CREATE TABLE IF NOT EXISTS journalpost_v1
(
    journalpostId   BIGINT PRIMARY KEY REFERENCES innsending_v1,
    status          VARCHAR(128)                NOT NULL,
    brukerId        VARCHAR(20)                 NULL,
    brukerType      VARCHAR(20)                 NULL,
    behandlingstema VARCHAR(20)                 NULL,
    registrertDato  TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS journalpost_dokumenter_v1
(
    journalpostId  BIGINT REFERENCES journalpost_v1,
    tittel         VARCHAR(50) NOT NULL,
    dokumentInfoId BIGINT      NOT NULL,
    brevkode       VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS soknad_v1
(
    journalpostId BIGINT REFERENCES innsending_v1,
    data          JSONB NOT NULL
);

CREATE TABLE IF NOT EXISTS person_v1
(
    id            BIGSERIAL PRIMARY KEY,
    fødselsnummer VARCHAR(11) NOT NULL,
    aktørId       VARCHAR(32) NOT NULL
);

CREATE TABLE IF NOT EXISTS person_innsending_v1
(
    journalpostId    BIGINT REFERENCES innsending_v1,
    personId         BIGSERIAL REFERENCES person_v1 (id),
    norsktilknytning BOOLEAN NOT NULL,
    diskresjonskode  BOOLEAN NOT NULL
);

CREATE TABLE IF NOT EXISTS aktivitetslogg_v1
(
    journalpostId BIGINT REFERENCES innsending_v1,
    data          JSONB NOT NULL
);