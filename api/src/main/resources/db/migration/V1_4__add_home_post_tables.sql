-- ============================================================
-- V1_4__add_home_post_tables.sql
-- Home feed: posts with text content and binary attachments
-- (images, PDFs, etc.)
-- ============================================================

-- ----------------------------------------------------------------
-- home_post  – the parent post record
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS home_post (
    id                 BIGSERIAL    PRIMARY KEY,
    title              VARCHAR(255),
    content            TEXT,
    author_id          BIGINT       NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         BIGINT       NOT NULL,
    last_modified_by   BIGINT
);

-- ----------------------------------------------------------------
-- post_attachment  – files attached to a post (images / PDFs)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS post_attachment (
    id           BIGSERIAL    PRIMARY KEY,
    post_id      BIGINT       NOT NULL REFERENCES home_post(id) ON DELETE CASCADE,
    data         BYTEA        NOT NULL,
    content_type VARCHAR(100),
    file_name    VARCHAR(255),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP    NOT NULL
);

