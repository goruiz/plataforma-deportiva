CREATE TABLE IF NOT EXISTS events_types (
    id          UUID         NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    description VARCHAR(255),
    created_by  UUID,
    created_at  TIMESTAMP    NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMP,
    deleted_by  UUID,
    deleted_at  TIMESTAMP
);

ALTER TABLE events
    ADD COLUMN IF NOT EXISTS id_event_type UUID,
    ADD CONSTRAINT fk_events_types_events
        FOREIGN KEY (id_event_type) REFERENCES events_types (id);
