-- ============================================================
-- V1__init_schema.sql
-- Full initial schema for the Bookshelf application.
-- Covers every entity class and their relationships.
-- The default schema (public) is configured via JPA properties.
-- ============================================================

-- ----------------------------------------------------------------
-- role
-- Table name derives from Hibernate default (class name lowercase)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS role (
    id                 BIGSERIAL    PRIMARY KEY,
    name               VARCHAR(50)  NOT NULL UNIQUE,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP
);

-- ----------------------------------------------------------------
-- _user  (name prefixed with underscore to avoid reserved word)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS _user (
    id                 BIGSERIAL    PRIMARY KEY,
    firstname          VARCHAR(255),
    lastname           VARCHAR(255),
    date_of_birth      DATE,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password           VARCHAR(255),
    provider           VARCHAR(50),
    account_locked     BOOLEAN      NOT NULL DEFAULT FALSE,
    enabled            BOOLEAN      NOT NULL DEFAULT FALSE,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP
);

-- ----------------------------------------------------------------
-- _user <-> role  (ManyToMany join table — Hibernate default name)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS _user_roles (
    users_id BIGINT NOT NULL REFERENCES _user(id) ON DELETE CASCADE,
    roles_id BIGINT NOT NULL REFERENCES role(id)  ON DELETE CASCADE,
    PRIMARY KEY (users_id, roles_id)
);

-- ----------------------------------------------------------------
-- token
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS token (
    id             BIGSERIAL    PRIMARY KEY,
    token          VARCHAR(255),
    created_at     TIMESTAMP,
    expires_at     TIMESTAMP,
    validated_at   TIMESTAMP,
    user_id        BIGINT       NOT NULL REFERENCES _user(id) ON DELETE CASCADE
);

-- ----------------------------------------------------------------
-- book
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS book (
    id                 BIGSERIAL    PRIMARY KEY,
    title              VARCHAR(255),
    author_name        VARCHAR(255),
    synopsis           TEXT,
    isbn               VARCHAR(255),
    genre              VARCHAR(255),
    cover              VARCHAR(255),
    cover_url          VARCHAR(255),
    page_bookmark      INTEGER,
    favourite          BOOLEAN      DEFAULT FALSE,
    archived           BOOLEAN      DEFAULT FALSE,
    shareable          BOOLEAN      DEFAULT FALSE,
    read               BOOLEAN      DEFAULT FALSE,
    owner_id           BIGINT       REFERENCES _user(id) ON DELETE SET NULL,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         BIGINT       NOT NULL,
    last_modified_by   BIGINT
);

-- ----------------------------------------------------------------
-- feedback  (book ratings / reviews)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS feedback (
    id                 BIGSERIAL    PRIMARY KEY,
    note               DOUBLE PRECISION,
    comment            VARCHAR(255),
    book_id            BIGINT       REFERENCES book(id) ON DELETE CASCADE,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         BIGINT       NOT NULL,
    last_modified_by   BIGINT
);

-- ----------------------------------------------------------------
-- book_transaction_history
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS book_transaction_history (
    id                 BIGSERIAL    PRIMARY KEY,
    user_id            BIGINT       REFERENCES _user(id) ON DELETE CASCADE,
    book_id            BIGINT       REFERENCES book(id)  ON DELETE CASCADE,
    returned           BOOLEAN      DEFAULT FALSE,
    return_approved    BOOLEAN      DEFAULT FALSE,
    requested          BOOLEAN      DEFAULT TRUE,
    request_approved   BOOLEAN      DEFAULT FALSE,
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         BIGINT       NOT NULL,
    last_modified_by   BIGINT
);

-- ----------------------------------------------------------------
-- app_feedback  (application-level feedback / bug reports)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_feedback (
    id                 BIGSERIAL    PRIMARY KEY,
    title              VARCHAR(255),
    description        TEXT,
    status             VARCHAR(20)  NOT NULL DEFAULT 'NEW'
                           CHECK (status IN ('NEW','IN_PROGRESS','RESOLVED','CLOSED')),
    created_date       TIMESTAMP    NOT NULL,
    last_modified_date TIMESTAMP,
    created_by         BIGINT       NOT NULL,
    last_modified_by   BIGINT
);

-- ----------------------------------------------------------------
-- app_feedback_upvotes  (@ElementCollection of user IDs)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_feedback_upvotes (
    feedback_id BIGINT NOT NULL REFERENCES app_feedback(id) ON DELETE CASCADE,
    user_id     BIGINT NOT NULL,
    PRIMARY KEY (feedback_id, user_id)
);

-- ----------------------------------------------------------------
-- app_feedback_comments  (@ElementCollection of @Embeddable)
-- ----------------------------------------------------------------
CREATE TABLE IF NOT EXISTS app_feedback_comments (
    feedback_id  BIGINT       NOT NULL REFERENCES app_feedback(id) ON DELETE CASCADE,
    author_id    BIGINT,
    author_name  VARCHAR(255),
    message      TEXT,
    created_at   TIMESTAMP
);

-- ================================================================
-- SEED DATA
-- ================================================================

-- Default roles (INSERT ... ON CONFLICT to make script idempotent)
INSERT INTO role (name, created_date) VALUES
  ('ROLE_USER',  NOW()),
  ('ROLE_ADMIN', NOW())
ON CONFLICT (name) DO NOTHING;

