CREATE TABLE IF NOT EXISTS users
(
    id               UUID         NOT NULL,
    id_role          UUID,
    first_name       VARCHAR(255),
    middle_name      VARCHAR(255),
    last_name        VARCHAR(255),
    second_last_name VARCHAR(255),
    username         VARCHAR(255),
    email            VARCHAR(255),
    password         VARCHAR(255),
    created_by       UUID,
    created_at       TIMESTAMP    NOT NULL,
    updated_by       UUID,
    updated_at       TIMESTAMP,
    deleted_by       UUID,
    deleted_at       TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

CREATE UNIQUE INDEX IF NOT EXISTS idx_users_email    ON users (email);
CREATE UNIQUE INDEX IF NOT EXISTS idx_users_username ON users (username);
