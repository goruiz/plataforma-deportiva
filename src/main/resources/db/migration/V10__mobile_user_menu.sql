-- Navigation for authenticated mobile users. The first bottom-navigation item is the home tab; the remaining
-- app-supported sections are arranged in the bottom bar or the home quick actions.
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, created_at, updated_at)
SELECT gen_random_uuid(), item.name, NULL, item.translation_key, NULL, item.icon, item.url,
       item.nav_order, item.item_order, true, false, false, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM (VALUES
        ('Inicio', 'menu.home', 'home', '/home', 1::smallint, 0),
        ('Ligas', 'menu.leagues.title', 'military_tech', '/ligas', 2::smallint, 1),
        ('Partidos', 'home.action_my_matches', 'sports_soccer', '/matches', NULL::smallint, 4),
        ('Rankings', 'menu.rankings', 'leaderboard', '/rankings', NULL::smallint, 5),
        ('Reportes', 'menu.reports', 'bar_chart', '/reports', NULL::smallint, 6),
        ('Perfil', 'menu.profile', 'person', '/profile', 5::smallint, 7)
) AS item (name, translation_key, icon, url, nav_order, item_order)
WHERE NOT EXISTS (
    SELECT 1 FROM menu existing
    WHERE existing.url = item.url AND existing.deleted_at IS NULL
);

-- These existing entries are also used by the Flutter app; nav_order is not used by the Angular sidebar.
UPDATE menu
SET nav_order = CASE url
        WHEN '/home' THEN 1
        WHEN '/ligas' THEN 2
        WHEN '/teams' THEN 3
        WHEN '/events' THEN 4
        WHEN '/profile' THEN 5
        ELSE NULL
    END,
    "order" = CASE url
        WHEN '/home' THEN 0
        WHEN '/ligas' THEN 1
        WHEN '/teams' THEN 2
        WHEN '/events' THEN 3
        WHEN '/matches' THEN 4
        WHEN '/rankings' THEN 5
        WHEN '/reports' THEN 6
        WHEN '/profile' THEN 7
    END
WHERE url IN ('/home', '/ligas', '/teams', '/events', '/matches', '/rankings', '/reports', '/profile')
  AND deleted_at IS NULL;

-- All non-administrative roles are mobile-user roles. Keep the platform roles limited to their existing panel menu.
UPDATE roles_menu assignment
SET visible = true,
    can_read = true,
    can_creat = false,
    can_update = false,
    can_delete = false,
    updated_at = LOCALTIMESTAMP(0)
FROM roles role
JOIN menu item ON item.url IN ('/home', '/ligas', '/teams', '/events', '/matches', '/rankings', '/reports', '/profile')
              AND item.deleted_at IS NULL
WHERE assignment.id_role = role.id
  AND assignment.id_menu = item.id
  AND assignment.deleted_at IS NULL
  AND role.deleted_at IS NULL
  AND role.id NOT IN ('${admin_role_id}'::uuid, '${owner_role_id}'::uuid);

INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), role.id, item.id, true, true, false, false, false,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM roles role
CROSS JOIN menu item
WHERE role.deleted_at IS NULL
  AND role.id NOT IN ('${admin_role_id}'::uuid, '${owner_role_id}'::uuid)
  AND item.url IN ('/home', '/ligas', '/teams', '/events', '/matches', '/rankings', '/reports', '/profile')
  AND item.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu existing
      WHERE existing.id_role = role.id
        AND existing.id_menu = item.id
        AND existing.deleted_at IS NULL
  );
