-- =====================================================================================================================
-- V9 · Menú en grupos y pantalla «Menús»
--
-- Hasta ahora todas las opciones del panel eran del mismo nivel. Esta migración las reparte en grupos (Competición,
-- Equipos y categorías, Instalaciones y Plataforma) y suma la pantalla donde el propietario crea, edita y ordena las
-- opciones del menú, con sus grupos, a la profundidad que haga falta.
--
--   1. `menu.is_system`: marca las pantallas que la plataforma necesita para gestionar el menú. No se pueden borrar,
--      ocultar ni cambiar su url (el propietario se quedaría sin la pantalla que sirve para arreglarlo).
--   2. La pantalla «Menús» (`/menus`, la ruta del panel), exclusiva del propietario.
--   3. Los grupos. Un grupo es una opción sin url que solo contiene otras: no lleva `roles_menu` propio, el menú lo
--      muestra mientras haya algo visible dentro (ver `MenuTree.visibleFor` en el backend).
--   4. Cada pantalla existente pasa a su grupo, con su lugar dentro. Solo se mueven las que siguen en el primer nivel:
--      una opción que ya se colocó a mano no se toca.
--
-- Es idempotente (IF NOT EXISTS, NOT EXISTS), como las anteriores. `nav_order` no se toca: la app móvil lo usa para su
-- barra inferior.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. Opciones del sistema
ALTER TABLE menu ADD COLUMN IF NOT EXISTS is_system BOOLEAN NOT NULL DEFAULT FALSE;


-- 2. La pantalla «Menús», exclusiva del propietario
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'Menús', 'Crear, editar y ordenar las opciones del menú',
       'menu.admin.menus', 'menu.admin.menus_description', 'cilSitemap', '/menus',
       11, 11, true, true, true, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.url = '/menus' AND m.deleted_at IS NULL);

-- Solo el rol Propietario la ve y puede usarla (el rol Administrador no la recibe)
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${owner_role_id}'::uuid, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
WHERE m.url = '/menus'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = '${owner_role_id}'::uuid AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );

-- Las dos pantallas que gestionan el menú
UPDATE menu
SET is_system = TRUE
WHERE url IN ('/menus', '/role-menus') AND deleted_at IS NULL AND is_system = FALSE;


-- 3. Los grupos, con su lugar entre las opciones del primer nivel (Dashboard 0, grupos 1 a 3, Usuarios 4, Plataforma 5)
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, created_at, updated_at)
SELECT gen_random_uuid(), g.name, g.description, g.name_translation_key, g.description_translation_key, g.icon, NULL,
       NULL, g.item_order, true, g.owner_only, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM (VALUES
        ('Competición', 'Eventos, formatos de juego y días sin juego',
            'menu.admin.group_competition', 'menu.admin.group_competition_description', 'cilSoccer', 1, false),
        ('Equipos y categorías', 'Equipos, sus jugadores y las categorías en que compiten',
            'menu.admin.group_teams', 'menu.admin.group_teams_description', 'cilGroup', 2, false),
        ('Instalaciones', 'Complejos deportivos y las canchas donde se juega',
            'menu.admin.group_facilities', 'menu.admin.group_facilities_description', 'cilHouse', 3, false),
        ('Plataforma', 'Lo que solo gestiona el propietario de la plataforma',
            'menu.admin.group_platform', 'menu.admin.group_platform_description', 'cilSettings', 5, true)
) AS g (name, description, name_translation_key, description_translation_key, icon, item_order, owner_only)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.name = g.name AND m.deleted_at IS NULL);


-- 4. Cada pantalla, dentro de su grupo
UPDATE menu m
SET id_parent_menu = parent.id, "order" = layout.item_order
FROM (VALUES
        ('/events',          'Competición',          0),
        ('/event-formats',   'Competición',          1),
        ('/holidays',        'Competición',          2),
        ('/teams',           'Equipos y categorías', 0),
        ('/categories',      'Equipos y categorías', 1),
        ('/sport-complexes', 'Instalaciones',        0),
        ('/courts',          'Instalaciones',        1),
        ('/organizations',   'Plataforma',           0),
        ('/role-menus',      'Plataforma',           1),
        ('/menus',           'Plataforma',           2)
) AS layout (url, group_name, item_order)
JOIN menu parent ON parent.name = layout.group_name AND parent.deleted_at IS NULL AND parent.url IS NULL
WHERE m.url = layout.url
  AND m.deleted_at IS NULL
  AND m.id_parent_menu IS NULL;

-- Las que se quedan en el primer nivel
UPDATE menu SET "order" = 0 WHERE url = '/dashboard' AND id_parent_menu IS NULL AND deleted_at IS NULL;
UPDATE menu SET "order" = 4 WHERE url = '/users'     AND id_parent_menu IS NULL AND deleted_at IS NULL;
