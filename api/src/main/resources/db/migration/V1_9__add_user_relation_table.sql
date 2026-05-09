-- ============================================================
-- V1_9__add_user_relation_table.sql
-- User relation system (friend requests, friendships, follows)
-- ============================================================

CREATE TABLE IF NOT EXISTS user_relation (
    id             BIGSERIAL    PRIMARY KEY,
    requester_id   BIGINT       NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    addressee_id   BIGINT       NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    relation_type  VARCHAR(30)  NOT NULL,   -- FRIEND_REQUEST | FOLLOW
    status         VARCHAR(20)  NOT NULL DEFAULT 'PENDING',  -- PENDING | ACCEPTED | REJECTED
    created_at     TIMESTAMP    NOT NULL DEFAULT NOW(),

    CONSTRAINT uq_user_relation UNIQUE (requester_id, addressee_id, relation_type)
);

CREATE INDEX IF NOT EXISTS idx_relation_addressee
    ON user_relation (addressee_id, relation_type, status);

CREATE INDEX IF NOT EXISTS idx_relation_requester
    ON user_relation (requester_id, relation_type, status);

