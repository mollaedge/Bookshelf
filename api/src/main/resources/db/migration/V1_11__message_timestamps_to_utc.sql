-- Migrate message and conversation timestamp columns from plain TIMESTAMP
-- (server-local time) to TIMESTAMP WITH TIME ZONE (UTC).
--
-- Existing rows are re-interpreted as being in Europe/Helsinki, which was the
-- server timezone when they were originally written.  New rows will be written
-- in UTC by the application (Instant.now()) and stored correctly.

ALTER TABLE message
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'Europe/Helsinki';

ALTER TABLE conversation
    ALTER COLUMN created_at TYPE TIMESTAMPTZ
        USING created_at AT TIME ZONE 'Europe/Helsinki';

ALTER TABLE conversation
    ALTER COLUMN last_message_at TYPE TIMESTAMPTZ
        USING last_message_at AT TIME ZONE 'Europe/Helsinki';

