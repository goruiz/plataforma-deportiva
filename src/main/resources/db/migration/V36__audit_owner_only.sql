-- =====================================================================================================================
-- V36 · La auditoría es solo del propietario de la plataforma
--
--   Muestra quién hizo qué en los datos de TODOS los clientes, así que ningún administrador de una organización debe
--   verla. `V30` la concedió a los dos roles del panel; esta migración la deja como una opción exclusiva del propietario
--   (`menu.owner_only`, que el backend ya no deja dar a otro rol) y le quita la asignación al rol Administrador. El
--   endpoint (`GET /audit`) responde 403 a cualquiera que no sea el propietario.
--
-- No se edita `V30` porque puede estar ya aplicada: una migración aplicada no se cambia. Es idempotente.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
-- =====================================================================================================================


-- 1. La opción pasa a ser exclusiva del propietario
UPDATE menu
SET owner_only = true,
    updated_at = LOCALTIMESTAMP(0)
WHERE url = '/audit'
  AND deleted_at IS NULL
  AND owner_only = false;


-- 2. El rol Administrador deja de tenerla (borrado lógico de su asignación, como `DELETE /roles-menu`)
UPDATE roles_menu
SET deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
WHERE id_role = '${admin_role_id}'::uuid
  AND deleted_at IS NULL
  AND id_menu IN (SELECT id FROM menu WHERE url = '/audit' AND deleted_at IS NULL);
