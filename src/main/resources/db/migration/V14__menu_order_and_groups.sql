-- =====================================================================================================================
-- V14 · Menú ordenado, con las opciones del rol «Usuario» en su propio grupo
--
-- La V9 repartió las pantallas del panel en grupos con su orden, pero la V10 sumó las de la app móvil sueltas en el primer
-- nivel y reescribió el `order` de /events y /teams (que ya estaban dentro de un grupo), así que el orden quedó repetido y
-- mezclado. Esta migración deja el árbol así (cada nivel con su `order` 0, 1, 2... sin repetir):
--
--   Dashboard
--   Competición           Eventos · Formatos de evento · Feriados
--   Equipos y categorías  Equipos · Categorías
--   Instalaciones         Complejos deportivos · Canchas
--   Usuarios
--   App móvil             Inicio · Liga · Liga · Torneo · Partidos · Rankings · Reportes · Perfil
--   Plataforma            Organizaciones · Menú por rol · Menús        (exclusivo del propietario)
--
--   1. El grupo «App móvil»: las opciones que son del rol «Usuario». El administrador no las recibe, así que no ve el
--      grupo; el propietario, que ve todo el menú, las ve agrupadas aparte de las pantallas del panel.
--   2. Cada opción, dentro de su grupo y en su lugar. Equipos, Eventos y Canchas las usan los dos clientes y se quedan en
--      los grupos del panel.
--   3. El primer nivel y los propios grupos.
--   4. `nav_order` (la barra inferior de la app móvil): solo Inicio, Liga, Equipos, Eventos y Perfil. Varias pantallas del
--      panel conservaban uno de la V1 y, desde que la V11 dio /courts a los usuarios, «Canchas» era un sexto botón; «Liga»
--      (/ranking) y «Torneo» (/torneo) tenían el 2 y el 1 de la barra anterior a la V10.
--   5. «Liga» (/ranking) y «Torneo» (/torneo), que ninguna migración crea, se conceden al rol «Usuario». La app aún no
--      tiene pantalla para ellas y muestra el marcador de módulo por registrar.
--
-- Los grupos se buscan por nombre y las pantallas por url, nunca por id. Una opción cuyo grupo ya no existe se deja donde
-- está. Es idempotente: reaplicarla deja el mismo árbol. Lo que ven los roles del panel (`roles_menu`) no cambia.
--
-- La clave de traducción del grupo se llama igual en los dos clientes, pero cada uno la redacta para su público: el panel
-- dice «App móvil» y la app dice «Mi competición».
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${mobile_user_role_id}  UUID del rol «Usuario»; es el mismo app.security.mobile-user-role-id.
-- =====================================================================================================================


-- 1. El grupo de las opciones del rol «Usuario»
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'App móvil', 'Lo que ve el rol Usuario en la app móvil: ligas, partidos, rankings y reportes',
       'menu.admin.group_mobile_app', 'menu.admin.group_mobile_app_description', 'cilMobile', NULL,
       NULL, 5, true, false, false, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.name = 'App móvil' AND m.deleted_at IS NULL);


-- 2. Cada opción, dentro de su grupo
UPDATE menu m
SET id_parent_menu = parent.id,
    "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/events',          'Competición',          0),
        ('/event-formats',   'Competición',          1),
        ('/holidays',        'Competición',          2),
        ('/teams',           'Equipos y categorías', 0),
        ('/categories',      'Equipos y categorías', 1),
        ('/sport-complexes', 'Instalaciones',        0),
        ('/courts',          'Instalaciones',        1),
        ('/home',            'App móvil',            0),
        ('/ligas',           'App móvil',            1),
        ('/ranking',         'App móvil',            2),
        ('/torneo',          'App móvil',            3),
        ('/matches',         'App móvil',            4),
        ('/rankings',        'App móvil',            5),
        ('/reports',         'App móvil',            6),
        ('/profile',         'App móvil',            7),
        ('/organizations',   'Plataforma',           0),
        ('/role-menus',      'Plataforma',           1),
        ('/menus',           'Plataforma',           2)
) AS layout (url, group_name, item_order)
JOIN menu parent ON parent.name = layout.group_name AND parent.url IS NULL AND parent.deleted_at IS NULL
WHERE m.url = layout.url
  AND m.deleted_at IS NULL;


-- 3. El primer nivel: pantallas sueltas y grupos
UPDATE menu m
SET "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/dashboard', 0),
        ('/users',     4)
) AS layout (url, item_order)
WHERE m.url = layout.url
  AND m.deleted_at IS NULL
  AND m.id_parent_menu IS NULL;

UPDATE menu m
SET "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('Competición',          1),
        ('Equipos y categorías', 2),
        ('Instalaciones',        3),
        ('App móvil',            5),
        ('Plataforma',           6)
) AS layout (group_name, item_order)
WHERE m.name = layout.group_name
  AND m.url IS NULL
  AND m.deleted_at IS NULL
  AND m.id_parent_menu IS NULL;


-- 4. La barra inferior de la app móvil: solo estas cinco, en este orden
UPDATE menu m
SET nav_order = bar.bar_slot,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/home',    1),
        ('/ligas',   2),
        ('/teams',   3),
        ('/events',  4),
        ('/profile', 5)
) AS bar (url, bar_slot)
WHERE m.url = bar.url
  AND m.deleted_at IS NULL
  AND m.nav_order IS DISTINCT FROM bar.bar_slot;

UPDATE menu
SET nav_order = NULL,
    updated_at = LOCALTIMESTAMP(0)
WHERE url IN ('/dashboard', '/users', '/event-formats', '/holidays', '/categories', '/sport-complexes', '/courts',
              '/matches', '/rankings', '/reports', '/ranking', '/torneo', '/organizations', '/role-menus', '/menus')
  AND nav_order IS NOT NULL
  AND deleted_at IS NULL;


-- 5. «Liga» (/ranking) y «Torneo» (/torneo) son del rol «Usuario»: solo lectura, como el resto de sus pantallas
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${mobile_user_role_id}'::uuid, item.id, true, true, false, false, false,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu item
WHERE item.url IN ('/ranking', '/torneo')
  AND item.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu existing
      WHERE existing.id_role = '${mobile_user_role_id}'::uuid
        AND existing.id_menu = item.id
        AND existing.deleted_at IS NULL
  );
