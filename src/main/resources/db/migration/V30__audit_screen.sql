-- =====================================================================================================================
-- V30 · Pantalla «Auditoría»: quién creó, cambió o borró cada registro
--
-- No hay tablas nuevas: cada tabla ya guarda quién creó, quién cambió por última vez y quién borró cada fila
-- (`created_by`, `updated_by`, `deleted_by` con sus fechas), y `GET /audit` los lee. Esta migración solo suma la opción
-- del menú:
--
--   1. La opción «Auditoría» (`/audit`, la ruta del panel), dentro del grupo «Usuarios y organizaciones», después de
--      «Organizaciones».
--   2. Concedida a los dos roles del panel, Administrador y Propietario. Lo que ve cada uno lo decide el backend: el
--      administrador, solo los cambios de su organización; el propietario, todos.
--
-- El grupo se busca por nombre y la pantalla por url, nunca por id. Es idempotente (NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. La opción «Auditoría», al final de «Usuarios y organizaciones»
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, id_parent_menu, created_at, updated_at)
SELECT gen_random_uuid(), 'Auditoría', 'Quién creó, cambió o borró cada registro y cuándo',
       'menu.admin.audit', 'menu.admin.audit_description', 'cilHistory', '/audit',
       NULL, COALESCE((SELECT MAX(sibling."order") + 1 FROM menu sibling
                       WHERE sibling.id_parent_menu = users_group.id AND sibling.deleted_at IS NULL), 0),
       true, false, false, users_group.id, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu users_group
WHERE users_group.name = 'Usuarios y organizaciones'
  AND users_group.url IS NULL
  AND users_group.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM menu m WHERE m.url = '/audit' AND m.deleted_at IS NULL);


-- 2. Los dos roles del panel la ven
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
