DO
$do$
    BEGIN
        IF EXISTS(
                SELECT
                FROM pg_catalog.pg_roles -- SELECT list can be empty for this
                WHERE rolname = 'bqconn_dataanalyse') THEN
            grant select on all tables in schema "public" to bqconn_dataanalyse;
        END IF;
    END
$do$;