-- =====================================================================================================================
-- V6 · Pantalla «Menú por rol»: qué opciones del menú ve cada rol
--
-- Es la pantalla donde el propietario de la plataforma decide, rol por rol, qué opciones del menú ve cada uno. Es una
-- pantalla del propietario: la ve solo su rol, y ningún otro rol puede recibirla.
--
--   1. `menu.owner_only`: marca las opciones que son del propietario. El backend no las deja asignar a ningún otro rol y
--      no deja quitárselas al propietario (si no, podría quedarse sin la pantalla que sirve para devolvérselas).
--   2. La opción del menú (`/role-menus`, la ruta del panel), concedida únicamente al rol Propietario.
--
-- Es idempotente (IF NOT EXISTS, NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. Opciones del menú que son solo del propietario
ALTER TABLE menu ADD COLUMN IF NOT EXISTS owner_only BOOLEAN NOT NULL DEFAULT FALSE;


-- 2. La opción «Menú por rol», al final del menú del panel
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, created_at, updated_at)
SELECT gen_random_uuid(), 'Menú por rol', 'Qué opciones del menú ve cada rol',
       'menu.admin.role_menus', 'menu.admin.role_menus_description', 'cilLockLocked', '/role-menus',
       9, 9, true, true, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.url = '/role-menus' AND m.deleted_at IS NULL);

-- Solo el rol Propietario la ve y puede usarla (a diferencia de las demás pantallas, el rol Administrador no la recibe).
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${owner_role_id}'::uuid, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
WHERE m.url = '/role-menus'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = '${owner_role_id}'::uuid AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );
