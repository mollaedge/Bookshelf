-- ============================================================
-- V1_5__add_book_pdf_table.sql
-- Per-book PDF storage and reading-progress pointer
-- ============================================================

-- ----------------------------------------------------------------
-- book_pdf  – stores the raw PDF binary for a book
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS book_pdf (
    id           BIGSERIAL    PRIMARY KEY,
    book_id      BIGINT       NOT NULL UNIQUE REFERENCES book(id) ON DELETE CASCADE,
    data         BYTEA        NOT NULL,
    content_type VARCHAR(100),
    file_name    VARCHAR(255),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP    NOT NULL
);

-- ----------------------------------------------------------------
-- Add pdf_page_pointer to book  – tracks the last page read
-- ----------------------------------------------------------------
ALTER TABLE book
    ADD COLUMN IF NOT EXISTS pdf_page_pointer INTEGER DEFAULT 1;

