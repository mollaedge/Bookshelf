-- ============================================================
-- V1_8__add_notification_table.sql
-- General notification system
-- ============================================================

CREATE TABLE IF NOT EXISTS notification (
    id             BIGSERIAL    PRIMARY KEY,
    recipient_id   BIGINT       NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    actor_id       BIGINT       REFERENCES _user(id) ON DELETE SET NULL,
    type           VARCHAR(60)  NOT NULL,
    title          VARCHAR(255) NOT NULL,
    message        TEXT,
    is_read        BOOLEAN      NOT NULL DEFAULT FALSE,
    reference_id   BIGINT,
    reference_type VARCHAR(50),
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_notification_recipient
    ON notification (recipient_id, created_at DESC);

CREATE INDEX IF NOT EXISTS idx_notification_unread
    ON notification (recipient_id, is_read)
    WHERE is_read = FALSE;

