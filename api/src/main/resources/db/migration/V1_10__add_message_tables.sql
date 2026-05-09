-- ============================================================
-- V1_10__add_message_tables.sql
-- Direct messaging between friends (conversations + messages)
-- ============================================================

-- Each unique pair of users shares exactly one conversation row.
-- user1_id is always the smaller of the two user IDs (normalisation)
-- so the UNIQUE constraint can do its job without duplicate rows.
CREATE TABLE IF NOT EXISTS conversation (
    id              BIGSERIAL   PRIMARY KEY,
    user1_id        BIGINT      NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    user2_id        BIGINT      NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW(),
    last_message_at TIMESTAMP,

    CONSTRAINT uq_conversation UNIQUE (user1_id, user2_id),
    CONSTRAINT chk_conversation_order CHECK (user1_id < user2_id)
);

CREATE INDEX IF NOT EXISTS idx_conversation_user1
    ON conversation (user1_id, last_message_at DESC NULLS LAST);
CREATE INDEX IF NOT EXISTS idx_conversation_user2
    ON conversation (user2_id, last_message_at DESC NULLS LAST);

-- Individual messages inside a conversation.
CREATE TABLE IF NOT EXISTS message (
    id              BIGSERIAL   PRIMARY KEY,
    conversation_id BIGINT      NOT NULL REFERENCES conversation(id) ON DELETE CASCADE,
    sender_id       BIGINT      NOT NULL REFERENCES _user(id)        ON DELETE CASCADE,
    content         TEXT        NOT NULL,
    is_read         BOOLEAN     NOT NULL DEFAULT FALSE,
    created_at      TIMESTAMP   NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_message_conversation
    ON message (conversation_id, created_at);
CREATE INDEX IF NOT EXISTS idx_message_unread
    ON message (conversation_id, is_read)
    WHERE is_read = FALSE;

