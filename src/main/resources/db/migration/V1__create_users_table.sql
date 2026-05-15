CREATE TABLE IF NOT EXISTS users (
    id               UUID NOT NULL,
    id_role          UUID,
    first_name       VARCHAR(255),
    middle_name      VARCHAR(255),
    last_name        VARCHAR(255),
    second_last_name VARCHAR(255),
    username         VARCHAR(255),
    email            VARCHAR(255),
    password         VARCHAR(255),
    created_by       UUID,
    created_at       TIMESTAMP(6),
    updated_by       UUID,
    updated_at       TIMESTAMP(6),
    deleted_by       UUID,
    deleted_at       TIMESTAMP(6),
    CONSTRAINT users_pkey1 PRIMARY KEY (id)
);
