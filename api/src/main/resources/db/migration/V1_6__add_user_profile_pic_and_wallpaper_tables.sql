CREATE TABLE user_profile_pic
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL UNIQUE,
    data         BYTEA         NOT NULL,
    content_type VARCHAR(100),
    file_name    VARCHAR(255),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP     NOT NULL
);

CREATE TABLE user_wallpaper
(
    id           BIGSERIAL PRIMARY KEY,
    user_id      BIGINT        NOT NULL UNIQUE,
    data         BYTEA         NOT NULL,
    content_type VARCHAR(100),
    file_name    VARCHAR(255),
    file_size    BIGINT,
    uploaded_at  TIMESTAMP     NOT NULL
);

