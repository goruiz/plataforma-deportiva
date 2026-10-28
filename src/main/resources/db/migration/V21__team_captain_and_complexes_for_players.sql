-- =====================================================================================================================
-- V21 · Capitán de equipo y «Complejos deportivos» en la app
--
--   1. `players.is_captain`: el administrador designa al capitán de cada equipo desde el panel y la app lo muestra.
--      Por ahora es solo una insignia: no da permisos. Un equipo tiene como mucho un capitán (índice único parcial), y
--      quien sale del equipo deja de serlo (lo hace el backend).
--   2. La opción «Complejos deportivos» (`/sport-complexes`, que el panel ya tiene desde la V2) pasa a verla también el
--      rol «Usuario», en solo lectura: la app muestra los complejos de su organización con su ubicación y sus canchas.
--      Queda en el grupo «Instalaciones», junto a «Canchas».
--
-- Es idempotente: reaplicarla no cambia nada. La opción se busca por url, nunca por id.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${mobile_user_role_id}  UUID del rol «Usuario»; es el mismo app.security.mobile-user-role-id.
-- =====================================================================================================================


-- 1. Capitán
ALTER TABLE players ADD COLUMN IF NOT EXISTS is_captain BOOLEAN NOT NULL DEFAULT false;

-- Parcial: solo los capitanes entran al índice, y un jugador borrado no ocupa el puesto
CREATE UNIQUE INDEX IF NOT EXISTS ux_players_team_captain
    ON players (id_team)
    WHERE is_captain = true AND deleted_at IS NULL;


-- 2. «Complejos deportivos» para el rol «Usuario», solo lectura
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${mobile_user_role_id}'::uuid, item.id, true, true, false, false, false,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu item
WHERE item.url = '/sport-complexes'
  AND item.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu existing
      WHERE existing.id_role = '${mobile_user_role_id}'::uuid
        AND existing.id_menu = item.id
        AND existing.deleted_at IS NULL
  );

-- Si ya existía una asignación oculta, se muestra
UPDATE roles_menu assignment
SET visible = true,
    can_read = true,
    updated_at = LOCALTIMESTAMP(0)
FROM menu item
WHERE assignment.id_menu = item.id
  AND item.url = '/sport-complexes'
  AND item.deleted_at IS NULL
  AND assignment.id_role = '${mobile_user_role_id}'::uuid
  AND assignment.deleted_at IS NULL
  AND (assignment.visible IS DISTINCT FROM true OR assignment.can_read IS DISTINCT FROM true);
