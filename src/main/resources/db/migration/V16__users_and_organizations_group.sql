-- =====================================================================================================================
-- V16 · «Usuarios y organizaciones», justo después del Dashboard
--
-- Usuarios estaba suelto en el primer nivel y Organizaciones dentro de «Plataforma». Las dos tratan de quién usa el
-- sistema (las cuentas y los clientes a los que pertenecen), así que van juntas en un grupo propio, en el segundo lugar
-- del menú:
--
--   Dashboard
--   Usuarios y organizaciones   Usuarios · Organizaciones      (Organizaciones sigue siendo exclusiva del propietario)
--   Competición                 Eventos · Formatos de evento · Feriados
--   Resultados y estadísticas   Liga · Partidos · Rankings · Reportes
--   Equipos y categorías        Equipos · Categorías
--   Instalaciones               Complejos deportivos · Canchas
--   App móvil                   Inicio · Liga (/ranking) · Torneo · Perfil
--   Plataforma                  Menú por rol · Menús           (exclusivo del propietario)
--
-- El grupo no es exclusivo del propietario: un grupo no lleva `roles_menu` propio y se muestra mientras algo de lo que
-- contiene sea visible para el rol (ver `MenuTree.visibleFor`), así que el administrador lo ve con Usuarios solamente y
-- el propietario con las dos. Lo que cada rol ve (`roles_menu`) no cambia.
--
-- Los grupos se buscan por nombre y las pantallas por url, nunca por id. Es idempotente, como las anteriores.
-- =====================================================================================================================


-- 1. El grupo
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'Usuarios y organizaciones', 'Las cuentas de la plataforma y los clientes a los que pertenecen',
       'menu.admin.group_users', 'menu.admin.group_users_description', 'cilUser', NULL,
       NULL, 1, true, false, false, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.name = 'Usuarios y organizaciones' AND m.deleted_at IS NULL);


-- 2. Usuarios y Organizaciones dentro del grupo; «Plataforma» conserva lo que gestiona el menú
UPDATE menu m
SET id_parent_menu = parent.id,
    "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/users',         'Usuarios y organizaciones', 0),
        ('/organizations', 'Usuarios y organizaciones', 1),
        ('/role-menus',    'Plataforma',                0),
        ('/menus',         'Plataforma',                1)
) AS layout (url, group_name, item_order)
JOIN menu parent ON parent.name = layout.group_name AND parent.url IS NULL AND parent.deleted_at IS NULL
WHERE m.url = layout.url
  AND m.deleted_at IS NULL;


-- 3. El primer nivel: el Dashboard y, a continuación, los grupos
UPDATE menu m
SET "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('Usuarios y organizaciones', 1),
        ('Competición',               2),
        ('Resultados y estadísticas', 3),
        ('Equipos y categorías',      4),
        ('Instalaciones',             5),
        ('App móvil',                 6),
        ('Plataforma',                7)
) AS layout (group_name, item_order)
WHERE m.name = layout.group_name
  AND m.url IS NULL
  AND m.deleted_at IS NULL
  AND m.id_parent_menu IS NULL;

UPDATE menu
SET "order" = 0,
    updated_at = LOCALTIMESTAMP(0)
WHERE url = '/dashboard'
  AND id_parent_menu IS NULL
  AND deleted_at IS NULL
  AND "order" IS DISTINCT FROM 0;
