-- =====================================================================================================================
-- V28 · Un jugador puede jugar en varios equipos, uno por organización
--
--   Hasta ahora `players.id_team` guardaba el único equipo de cada jugador. Ahora cada pertenencia a un equipo es una fila
--   de `team_members` y el jugador (la persona: su correo, su nombre, su foto) sigue siendo una sola fila de `players`.
--
--   · La regla «un equipo por organización» la sostiene un índice único parcial sobre (jugador, organización del equipo):
--     los equipos sin organización (los del propietario de la plataforma) cuentan como una organización más.
--   · Un equipo tiene como mucho un capitán: es el índice que estaba en `players`, ahora sobre `team_members`.
--   · Quien sale de un equipo deja su fila con `deleted_at` (borrado lógico, como el resto del sistema): así queda
--     constancia de quién jugó en cada equipo, y el índice solo mira las filas vigentes.
--   · Los datos de hoy pasan tal cual: cada jugador con equipo recibe su fila, con su capitanía.
--   · `players.id_team` y `players.is_captain` se eliminan: dejar una copia sería tener dos verdades.
--
-- Es idempotente: si `players.id_team` ya no existe, no hace nada.
-- =====================================================================================================================


CREATE TABLE IF NOT EXISTS team_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    player_id       UUID NOT NULL REFERENCES players (id) ON DELETE CASCADE,
    team_id         UUID NOT NULL REFERENCES teams (id),
    -- La del equipo, copiada para que «un equipo por organización» sea un índice; null = la del propietario.
    organization_id UUID,
    is_captain      BOOLEAN NOT NULL DEFAULT false,
    created_by      UUID,
    created_at      TIMESTAMP(6) NOT NULL,
    updated_by      UUID,
    updated_at      TIMESTAMP(6),
    deleted_by      UUID,
    deleted_at      TIMESTAMP(6)
);

-- Un equipo por organización y jugador. El UUID de ceros hace de «organización del propietario»: dos null no chocarían.
CREATE UNIQUE INDEX IF NOT EXISTS ux_team_members_player_organization
    ON team_members (player_id, COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid))
    WHERE deleted_at IS NULL;

-- Parcial: solo los capitanes entran al índice, y quien sale del equipo no ocupa el puesto
CREATE UNIQUE INDEX IF NOT EXISTS ux_team_members_team_captain
    ON team_members (team_id)
    WHERE is_captain = true AND deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_team_members_team ON team_members (team_id) WHERE deleted_at IS NULL;
CREATE INDEX IF NOT EXISTS idx_team_members_player ON team_members (player_id) WHERE deleted_at IS NULL;


-- Los datos de hoy: un jugador con equipo (y sin borrar) pasa a ser una fila de pertenencia
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'players' AND column_name = 'id_team') THEN

        INSERT INTO team_members (id, player_id, team_id, organization_id, is_captain, created_at, updated_at)
        SELECT gen_random_uuid(), p.id, p.id_team, t.organization_id, p.is_captain,
               COALESCE(p.updated_at, p.created_at), COALESCE(p.updated_at, p.created_at)
        FROM players p
        JOIN teams t ON t.id = p.id_team
        WHERE p.deleted_at IS NULL
          AND NOT EXISTS (
              SELECT 1 FROM team_members m
              WHERE m.player_id = p.id AND m.team_id = p.id_team AND m.deleted_at IS NULL
          );

        DROP INDEX IF EXISTS ux_players_team_captain;
        ALTER TABLE players DROP COLUMN id_team;
        ALTER TABLE players DROP COLUMN IF EXISTS is_captain;
    END IF;
END $$;
