-- ============================================================
-- V1_7__add_post_social_tables.sql
-- Likes, comments, and shares on home-feed posts
-- ============================================================

-- ----------------------------------------------------------------
-- post_like  – one row per (user × post) pair
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_like (
    id         BIGSERIAL  PRIMARY KEY,
    post_id    BIGINT     NOT NULL REFERENCES home_post(id) ON DELETE CASCADE,
    user_id    BIGINT     NOT NULL REFERENCES _user(id)     ON DELETE CASCADE,
    created_at TIMESTAMP  NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_post_like UNIQUE (post_id, user_id)
);

-- ----------------------------------------------------------------
-- post_comment  – comments on a post (full audit trail)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_comment (
    id                 BIGSERIAL  PRIMARY KEY,
    post_id            BIGINT     NOT NULL REFERENCES home_post(id) ON DELETE CASCADE,
    author_id          BIGINT     NOT NULL REFERENCES _user(id)     ON DELETE CASCADE,
    content            TEXT       NOT NULL,
    created_date       TIMESTAMP  NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         BIGINT     NOT NULL,
    last_modified_by   BIGINT
);

-- ----------------------------------------------------------------
-- post_share  – one row each time a user records a share
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_share (
    id         BIGSERIAL  PRIMARY KEY,
    post_id    BIGINT     NOT NULL REFERENCES home_post(id) ON DELETE CASCADE,
    user_id    BIGINT     NOT NULL REFERENCES _user(id)     ON DELETE CASCADE,
    shared_at  TIMESTAMP  NOT NULL DEFAULT NOW()
);

