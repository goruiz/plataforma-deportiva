INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, created_at, updated_at)
SELECT gen_random_uuid(), 'Canchas', NULL, 'menu.courts', NULL, 'location_on', '/courts',
       NULL, 8, true, false, false, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (
    SELECT 1 FROM menu existing
    WHERE existing.url = '/courts' AND existing.deleted_at IS NULL
);

UPDATE menu
SET name_translation_key = 'menu.courts',
    updated_at = LOCALTIMESTAMP(0)
WHERE url = '/courts'
  AND deleted_at IS NULL;

UPDATE roles_menu assignment
SET visible = true,
    can_read = true,
    can_creat = false,
    can_update = false,
    can_delete = false,
    updated_at = LOCALTIMESTAMP(0)
FROM roles role
JOIN menu item ON item.url = '/courts' AND item.deleted_at IS NULL
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
  AND item.url = '/courts'
  AND item.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu existing
      WHERE existing.id_role = role.id
        AND existing.id_menu = item.id
        AND existing.deleted_at IS NULL
  );
