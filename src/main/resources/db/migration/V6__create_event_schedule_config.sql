CREATE TABLE event_schedule_config (
    id            UUID         NOT NULL DEFAULT gen_random_uuid(),
    event_id      UUID         NOT NULL,
    play_days     VARCHAR(100) NOT NULL,
    start_time    TIME         NOT NULL,
    match_duration_minutes          INTEGER NOT NULL,
    break_between_halves_minutes    INTEGER NOT NULL DEFAULT 0,
    break_between_matches_minutes   INTEGER NOT NULL,
    court_id      UUID,
    created_by    UUID,
    created_at    TIMESTAMP    NOT NULL,
    updated_by    UUID,
    updated_at    TIMESTAMP,
    deleted_by    UUID,
    deleted_at    TIMESTAMP,
    CONSTRAINT pk_event_schedule_config PRIMARY KEY (id),
    CONSTRAINT fk_event_schedule_config_event  FOREIGN KEY (event_id)  REFERENCES events (id),
    CONSTRAINT fk_event_schedule_config_court  FOREIGN KEY (court_id)  REFERENCES courts (id)
);
