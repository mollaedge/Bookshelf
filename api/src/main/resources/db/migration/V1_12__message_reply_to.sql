-- ============================================================
-- V1_12__message_reply_to.sql
-- Allows a message to reference the message it is replying to.
-- NULL = top-level message (no reply context).
-- ON DELETE SET NULL: if the original message is deleted the
--   reply still exists but loses its quoted context gracefully.
-- ============================================================

ALTER TABLE message
    ADD COLUMN reply_to_id BIGINT
        REFERENCES message(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_message_reply_to
    ON message (reply_to_id)
    WHERE reply_to_id IS NOT NULL;

