-- =====================================================================================================================
-- V20 · Qué ve cada jugador: su organización y los eventos públicos
--
-- Hasta ahora una cuenta de la app sin organización veía los datos de todas. Desde esta versión un jugador ve lo de su
-- organización, que es la de su equipo, y los eventos que el organizador marque como públicos; el filtro lo aplica el
-- backend (ver `TenantFilters`).
--
--   1. `events.is_public`: el organizador decide qué eventos ve cualquier jugador (con sus equipos, partidos y tablas).
--      Todo evento existente queda privado.
--   2. Las cuentas de jugadores toman la organización de su equipo. El backend lo mantiene desde ahora (registrarse con
--      invitación, aceptar una, que lo agreguen a un equipo o lo saquen de él); esto alinea las cuentas que ya existían.
--      No toca a los administradores ni al propietario. Una cuenta cuyo equipo no tiene organización (es del
--      propietario) queda como estaba.
--
-- Es idempotente: reaplicarla no cambia nada.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. Eventos públicos
ALTER TABLE events ADD COLUMN IF NOT EXISTS is_public BOOLEAN NOT NULL DEFAULT false;

-- Parcial porque los públicos son pocos y el filtro por organización los busca en una subconsulta en cada lectura
CREATE INDEX IF NOT EXISTS idx_events_public ON events (id) WHERE is_public = true AND deleted_at IS NULL;


-- 2. La organización de cada jugador es la de su equipo
UPDATE users account
SET organization_id = team.organization_id,
    updated_at = LOCALTIMESTAMP(0)
FROM players player
JOIN teams team ON team.id = player.id_team AND team.deleted_at IS NULL
WHERE player.email = account.email
  AND player.deleted_at IS NULL
  AND account.deleted_at IS NULL
  AND team.organization_id IS NOT NULL
  AND account.organization_id IS DISTINCT FROM team.organization_id
  AND NOT EXISTS (
      SELECT 1 FROM users_roles assignment
      WHERE assignment.id_user = account.id
        AND assignment.deleted_at IS NULL
        AND assignment.id_role IN ('${admin_role_id}'::uuid, '${owner_role_id}'::uuid)
  );
