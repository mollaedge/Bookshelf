-- ============================================================
-- V2__add_user_profile_fields.sql
-- Adds bio and location columns to the _user table for the
-- enriched user dashboard feature.
-- The default schema (public) is configured via JPA properties.
-- ============================================================

ALTER TABLE _user
    ADD COLUMN IF NOT EXISTS bio      TEXT,
    ADD COLUMN IF NOT EXISTS location VARCHAR(255);
