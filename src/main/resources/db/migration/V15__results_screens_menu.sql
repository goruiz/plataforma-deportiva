-- =====================================================================================================================
-- V15 · Pantallas de resultados en el panel
--
-- Liga, Partidos, Rankings y Reportes eran solo de la app móvil. Ahora el panel también las tiene (en solo lectura): el
-- administrador ve lo de su organización y el propietario lo de todo el sistema, separado por administrador. Esa
-- separación la hace el backend (cada administrador solo recibe lo suyo); aquí solo se decide quién ve cada opción.
--
--   1. El grupo «Resultados y estadísticas», que reúne las cuatro. Un grupo no lleva `roles_menu` propio: se muestra
--      mientras algo de lo que contiene sea visible para el rol (ver `MenuTree.visibleFor`).
--   2. Las cuatro opciones salen de «App móvil» y entran en el grupo nuevo.
--   3. Se conceden a los roles Administrador y Propietario (el rol «Usuario» ya las tenía): solo lectura.
--   4. El orden del primer nivel y de «App móvil», que se queda con lo exclusivo del rol «Usuario».
--
-- Árbol resultante:
--
--   Dashboard
--   Competición             Eventos · Formatos de evento · Feriados
--   Resultados y estadísticas  Liga · Partidos · Rankings · Reportes
--   Equipos y categorías    Equipos · Categorías
--   Instalaciones           Complejos deportivos · Canchas
--   Usuarios
--   App móvil               Inicio · Liga (/ranking) · Torneo · Perfil
--   Plataforma              Organizaciones · Menú por rol · Menús
--
-- Los grupos se buscan por nombre y las pantallas por url, nunca por id. Es idempotente, como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. El grupo
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'Resultados y estadísticas', 'Ligas, partidos, rankings y reportes de solo lectura',
       'menu.admin.group_results', 'menu.admin.group_results_description', 'cilChart', NULL,
       NULL, 2, true, false, false, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.name = 'Resultados y estadísticas' AND m.deleted_at IS NULL);


-- 2. Las cuatro opciones dentro del grupo; «App móvil» conserva lo suyo
UPDATE menu m
SET id_parent_menu = parent.id,
    "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/ligas',    'Resultados y estadísticas', 0),
        ('/matches',  'Resultados y estadísticas', 1),
        ('/rankings', 'Resultados y estadísticas', 2),
        ('/reports',  'Resultados y estadísticas', 3),
        ('/home',     'App móvil',                 0),
        ('/ranking',  'App móvil',                 1),
        ('/torneo',   'App móvil',                 2),
        ('/profile',  'App móvil',                 3)
) AS layout (url, group_name, item_order)
JOIN menu parent ON parent.name = layout.group_name AND parent.url IS NULL AND parent.deleted_at IS NULL
WHERE m.url = layout.url
  AND m.deleted_at IS NULL;


-- «App móvil» ya no lleva las cuatro: su descripción dice lo que le queda
UPDATE menu
SET description = 'Lo que ve solo el rol Usuario en la app móvil: inicio, ligas, torneos y perfil',
    updated_at = LOCALTIMESTAMP(0)
WHERE name = 'App móvil'
  AND url IS NULL
  AND deleted_at IS NULL
  AND description IS DISTINCT FROM 'Lo que ve solo el rol Usuario en la app móvil: inicio, ligas, torneos y perfil';


-- 3. Los dos roles del panel las ven, en solo lectura
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), panel_role.id, item.id, true, true, false, false, false,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM (VALUES ('${admin_role_id}'::uuid), ('${owner_role_id}'::uuid)) AS panel_role (id)
CROSS JOIN menu item
WHERE item.url IN ('/ligas', '/matches', '/rankings', '/reports')
  AND item.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu existing
      WHERE existing.id_role = panel_role.id
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
  AND item.url IN ('/ligas', '/matches', '/rankings', '/reports')
  AND item.deleted_at IS NULL
  AND assignment.id_role IN ('${admin_role_id}'::uuid, '${owner_role_id}'::uuid)
  AND assignment.deleted_at IS NULL
  AND (assignment.visible IS DISTINCT FROM true OR assignment.can_read IS DISTINCT FROM true);


-- 4. El primer nivel
UPDATE menu m
SET "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/dashboard', 0),
        ('/users',     5)
) AS layout (url, item_order)
WHERE m.url = layout.url
  AND m.deleted_at IS NULL
  AND m.id_parent_menu IS NULL;

UPDATE menu m
SET "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('Competición',               1),
        ('Resultados y estadísticas', 2),
        ('Equipos y categorías',      3),
        ('Instalaciones',             4),
        ('App móvil',                 6),
        ('Plataforma',                7)
) AS layout (group_name, item_order)
WHERE m.name = layout.group_name
  AND m.url IS NULL
  AND m.deleted_at IS NULL
  AND m.id_parent_menu IS NULL;
