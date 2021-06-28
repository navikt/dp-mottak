ALTER TABLE person_v1
DROP CONSTRAINT IF EXISTS  person_v1_aktørid_key;
ALTER TABLE person_v1
DROP CONSTRAINT IF EXISTS  person_v1_fødselsnummer_key;

ALTER TABLE person_v1
RENAME COLUMN fødselsnummer TO ident;

ALTER TABLE person_v1
ADD CONSTRAINT person_v1_unique UNIQUE (ident, aktørid)