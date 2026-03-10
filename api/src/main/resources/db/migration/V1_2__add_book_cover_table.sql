-- ============================================================
-- V1_2__add_book_cover_table.sql
-- Adds a dedicated book_cover table to store book cover images
-- as binary large objects (LOB) in the database, replacing
-- the local filesystem storage approach.
-- ============================================================

CREATE TABLE IF NOT EXISTS book_cover (
    id           BIGSERIAL    PRIMARY KEY,
    book_id      BIGINT       NOT NULL UNIQUE REFERENCES book(id) ON DELETE CASCADE,
    data         BYTEA        NOT NULL,
    content_type VARCHAR(100),
    file_name    VARCHAR(255),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP    NOT NULL
);

