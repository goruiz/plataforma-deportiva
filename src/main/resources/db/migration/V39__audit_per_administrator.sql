-- =====================================================================================================================
-- V39 · La auditoría vuelve a cada administrador
--
--   `V36` la dejó exclusiva del propietario. Se pidió una auditoría POR ADMINISTRADOR: cada administrador ve qué se
--   creó, cambió o borró en SU organización (el backend ya acota la consulta a su organización) y el propietario ve la
--   de todos y puede elegir de qué administrador. Esta migración deshace lo de `V36` en el menú:
--
--   1. La opción «Auditoría» (`/audit`) deja de ser exclusiva del propietario (`menu.owner_only = false`).
--   2. El rol Administrador la recibe de nuevo (si su asignación estaba borrada se reactiva; si nunca existió se crea).
--      El rol Propietario conserva la suya.
--
-- No se edita `V36` porque ya puede estar aplicada. Es idempotente.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. La opción deja de ser exclusiva del propietario
UPDATE menu
SET owner_only = false,
    updated_at = LOCALTIMESTAMP(0)
WHERE url = '/audit'
  AND deleted_at IS NULL
  AND owner_only = true;


-- 2. El rol Administrador vuelve a tenerla: se reactiva la asignación borrada por `V36`...
UPDATE roles_menu
SET deleted_at = NULL,
    updated_at = LOCALTIMESTAMP(0)
WHERE id_role = '${admin_role_id}'::uuid
  AND deleted_at IS NOT NULL
  AND id_menu IN (SELECT id FROM menu WHERE url = '/audit' AND deleted_at IS NULL)
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu live
      WHERE live.id_role = roles_menu.id_role AND live.id_menu = roles_menu.id_menu AND live.deleted_at IS NULL
  );


-- ...y, si nunca existió, se crea (por ejemplo en una base donde `V30` no la concedió)
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), granted.role_id, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
CROSS JOIN (VALUES ('${admin_role_id}'::uuid), ('${owner_role_id}'::uuid)) AS granted (role_id)
WHERE m.url = '/audit'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = granted.role_id AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );
