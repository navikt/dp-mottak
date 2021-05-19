ALTER TABLE IF EXISTS journalpost_dokumenter_v1
    ADD COLUMN hovedDokument BOOL DEFAULT FALSE;


UPDATE journalpost_dokumenter_v1
SET hovedDokument = true
WHERE brevkode IN (
                   'NAV 04-02.01',
                   'NAVe 04-02.01',
                   'NAV 04-02.03',
                   'NAV 04-02.05',
                   'NAVe 04-02.05'
                    'NAV 04-01.04',
                   'NAVe 04-01.04',
                   'NAV 04-16.04',
                   'NAVe 04-16.04',
                   'NAVe 04-08.04',
                   'NAV 04-08.04'
                   'NAV 04-01.03',
                   'NAV 04-01.04'
                   'NAV 04-16.03',
                   'NAV 04-16.04'
                    'NAV 04-06.05'
                    'NAV 04-06.08'
                    'NAV 90-00.08'
                    'NAVe 04-16.04',
                   'NAVe 04-16.03',
                   'NAVe 04-01.03',
                   'NAVe 04-01.04'
    );
