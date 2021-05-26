ALTER TABLE IF EXISTS arenasak_v1
  RENAME COLUMN fagsakId TO oppgaveId_temp;

ALTER TABLE IF EXISTS arenasak_v1
    RENAME COLUMN oppgaveId TO fagsakId;

ALTER TABLE IF EXISTS arenasak_v1
    RENAME COLUMN oppgaveId_temp TO oppgaveId;