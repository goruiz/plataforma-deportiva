-- =====================================================================================================================
-- V7 · Pantalla «Organizaciones»: los clientes de la plataforma
--
-- Es la pantalla donde el propietario de la plataforma da de alta a sus clientes (una liga, un estadio, un complejo),
-- los edita, los suspende o los elimina. Como «Menú por rol» (V6), es una pantalla del propietario: la ve solo su rol y
-- ningún otro rol puede recibirla (`menu.owner_only`).
--
-- Es idempotente (NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. La opción «Organizaciones», al final del menú del panel
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, created_at, updated_at)
SELECT gen_random_uuid(), 'Organizaciones', 'Los clientes de la plataforma y su estado',
       'menu.admin.organizations', 'menu.admin.organizations_description', 'cilBuilding', '/organizations',
       10, 10, true, true, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.url = '/organizations' AND m.deleted_at IS NULL);


-- 2. Solo el rol Propietario la ve y puede usarla (el rol Administrador no la recibe)
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${owner_role_id}'::uuid, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
WHERE m.url = '/organizations'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = '${owner_role_id}'::uuid AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );
