CREATE TABLE IF NOT EXISTS team_invitations (
    id            UUID         NOT NULL DEFAULT gen_random_uuid() PRIMARY KEY,
    email         VARCHAR(255) NOT NULL,
    team_id       UUID         NOT NULL,
    token         VARCHAR(500) UNIQUE,
    type          VARCHAR(20)  NOT NULL,
    status        VARCHAR(20)  NOT NULL DEFAULT 'PENDING',
    expires_at    TIMESTAMP    NOT NULL,
    created_by    UUID,
    created_at    TIMESTAMP    NOT NULL,
    updated_by    UUID,
    updated_at    TIMESTAMP,
    deleted_by    UUID,
    deleted_at    TIMESTAMP,
    CONSTRAINT fk_teams_team_invitations FOREIGN KEY (team_id) REFERENCES teams (id)
);
