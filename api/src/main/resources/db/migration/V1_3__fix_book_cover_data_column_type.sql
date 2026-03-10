-- ============================================================
-- V1_3__fix_book_cover_data_column_type.sql
-- Fixes the book_cover.data column type from OID to BYTEA.
-- The @Lob annotation on a byte[] in Hibernate/PostgreSQL maps
-- to OID, but we want BYTEA for direct binary storage.
-- Removing @Lob from the entity requires the column to be BYTEA.
-- ============================================================

-- Recreate with correct BYTEA type (safe — table has no prod data)
DROP TABLE IF EXISTS book_cover;

CREATE TABLE IF NOT EXISTS book_cover (
    id           BIGSERIAL    PRIMARY KEY,
    book_id      BIGINT       NOT NULL UNIQUE REFERENCES book(id) ON DELETE CASCADE,
    data         BYTEA        NOT NULL,
    content_type VARCHAR(100),
    file_name    VARCHAR(255),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP    NOT NULL
);

