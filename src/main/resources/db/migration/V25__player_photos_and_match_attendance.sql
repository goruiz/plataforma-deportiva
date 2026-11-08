-- =====================================================================================================================
-- V25 · Foto de perfil del jugador, asistencia a partidos e índices de las consultas nuevas
--
--   1. `player_photos`: la imagen de perfil de un jugador, guardada en la propia base (sin almacenamiento en la nube,
--      que tiene costo). Una fila por jugador; al borrar al jugador de verdad se borra su foto.
--   2. `match_attendances`: «voy / no voy» de un jugador a un partido de su equipo. Una respuesta activa por jugador y
--      partido (cambiarla actualiza la fila; quitarla es un borrado lógico). Hereda la organización del partido, igual
--      que `match_events`.
--   3. Índices para lo que ahora se consulta por jugador y por cancha: los totales de un jugador leen sus eventos de
--      partido, y los partidos de una cancha (y sus horarios libres) se buscan por cancha y fecha.
--
-- Es idempotente: reaplicarla no cambia nada.
-- =====================================================================================================================


-- 1. Foto de perfil
CREATE TABLE IF NOT EXISTS player_photos (
    id            UUID PRIMARY KEY,
    player_id     UUID NOT NULL UNIQUE REFERENCES players(id) ON DELETE CASCADE,
    content_type  VARCHAR(50) NOT NULL,
    size_bytes    INTEGER NOT NULL,
    data          BYTEA NOT NULL,
    created_by    UUID,
    created_at    TIMESTAMP(6) NOT NULL,
    updated_by    UUID,
    updated_at    TIMESTAMP(6),
    deleted_by    UUID,
    deleted_at    TIMESTAMP(6)
);


-- 2. Asistencia a partidos
CREATE TABLE IF NOT EXISTS match_attendances (
    id          UUID PRIMARY KEY,
    match_id    UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    player_id   UUID NOT NULL REFERENCES players(id) ON DELETE CASCADE,
    -- El equipo por el que responde: si el jugador cambia de equipo, su respuesta sigue siendo la de aquel partido
    team_id     UUID NOT NULL,
    status      VARCHAR(20) NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMP(6),
    deleted_by  UUID,
    deleted_at  TIMESTAMP(6)
);

-- Parcial: quitar la respuesta (borrado lógico) deja al jugador volver a responder
CREATE UNIQUE INDEX IF NOT EXISTS ux_match_attendances_match_player
    ON match_attendances (match_id, player_id)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_match_attendances_match_id ON match_attendances (match_id);


-- 3. Índices de las consultas nuevas
CREATE INDEX IF NOT EXISTS idx_match_events_player_id ON match_events (player_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_matches_court_date ON matches (id_court, match_date) WHERE deleted_at IS NULL;
