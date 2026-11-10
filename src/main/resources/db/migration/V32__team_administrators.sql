-- =====================================================================================================================
-- V32 · Administrador del equipo
--
--   Además del capitán, un equipo puede tener administradores: jugadores a los que la plataforma da la gestión de SU
--   equipo (el código para unirse). No son administradores de la plataforma ni entran al panel.
--
--   · `team_members.is_team_admin`: es de cada pertenencia (de cada equipo), como `is_captain`, y a diferencia del capitán
--     puede haber varios por equipo, así que no lleva índice único.
--   · Quien sale del equipo deja de serlo: la fila queda borrada (`deleted_at`) y el código la ignora.
--   · El código pregunta «¿es administrador de este equipo?» por el correo de la cuenta y el equipo; el índice parcial
--     cubre esa consulta sin leer la tabla entera.
--
-- Es idempotente (IF NOT EXISTS), como las anteriores.
-- =====================================================================================================================


ALTER TABLE team_members ADD COLUMN IF NOT EXISTS is_team_admin BOOLEAN NOT NULL DEFAULT false;

CREATE INDEX IF NOT EXISTS idx_team_members_team_admin
    ON team_members (team_id)
    WHERE is_team_admin = true AND deleted_at IS NULL;
