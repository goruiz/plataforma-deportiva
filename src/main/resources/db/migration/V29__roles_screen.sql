-- =====================================================================================================================
-- V29 · Pantalla «Roles y permisos»: los roles de la plataforma y sus permisos
--
-- Es la pantalla donde el propietario de la plataforma crea, renombra y elimina roles y escribe los permisos de cada
-- uno. Los endpoints ya existían (`/roles` y `/permissions`, cuyas escrituras son solo del propietario); esta migración
-- solo suma la opción del menú. Como «Menú por rol» (V6) y «Menús» (V9), es una pantalla del propietario: la ve solo su
-- rol y ningún otro rol puede recibirla (`menu.owner_only`).
--
--   1. La opción «Roles y permisos» (`/roles`, la ruta del panel), dentro del grupo «Plataforma», después de «Menús».
--   2. Concedida únicamente al rol Propietario.
--
-- El grupo se busca por nombre y la pantalla por url, nunca por id. Es idempotente (NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. La opción «Roles y permisos», al final de «Plataforma»
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, id_parent_menu, created_at, updated_at)
SELECT gen_random_uuid(), 'Roles y permisos', 'Crear y renombrar roles y escribir los permisos de cada uno',
       'menu.admin.roles', 'menu.admin.roles_description', 'cilBadge', '/roles',
       NULL, COALESCE((SELECT MAX(sibling."order") + 1 FROM menu sibling
                       WHERE sibling.id_parent_menu = platform_group.id AND sibling.deleted_at IS NULL), 0),
       true, true, false, platform_group.id, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu platform_group
WHERE platform_group.name = 'Plataforma'
  AND platform_group.url IS NULL
  AND platform_group.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM menu m WHERE m.url = '/roles' AND m.deleted_at IS NULL);


-- 2. Solo el rol Propietario la ve y puede usarla (el rol Administrador no la recibe)
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${owner_role_id}'::uuid, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
WHERE m.url = '/roles'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = '${owner_role_id}'::uuid AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );
