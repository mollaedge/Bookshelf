-- V1_13__add_message_media_fields.sql
-- Add media fields to message table for media upload support

ALTER TABLE message
    ADD COLUMN media_data BYTEA,
    ADD COLUMN media_type VARCHAR(255),
    ADD COLUMN media_name VARCHAR(255),
    ADD COLUMN media_size BIGINT;

