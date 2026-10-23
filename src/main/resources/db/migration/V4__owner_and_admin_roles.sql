-- =====================================================================================================================
-- V4 · Dos roles independientes: Propietario y Administrador
--
-- La V3 dio el rol del propietario de la plataforma (el vendedor del sistema) a los administradores que ya existían,
-- que así quedaron con los dos roles. Aquí se separan:
--
--   · «Propietario» (app.security.owner-role-id): ve todas las organizaciones. Por sí solo le da entrada al panel: el
--     login lo trata como administrador sin necesitar el otro rol.
--   · «Administrador» (app.security.admin-role-id): el de cada cliente, que solo ve lo de su organización.
--
-- Ninguno incluye al otro. Pasos:
--   1. Nombres visibles de los dos roles (los roles se buscan por id, nunca por nombre).
--   2. Quitar el rol Administrador a quien tiene el de Propietario.
--   3. Darle al Propietario el mismo menú que tiene el Administrador, porque el menú se concede por rol.
--
-- Una pantalla nueva del panel debe concederse en `roles_menu` a los dos roles (la del propietario solo, si es suya).
-- Es idempotente, como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. Nombres visibles
UPDATE roles
SET name = 'Administrador',
    description = 'Administra los campeonatos de su organización',
    updated_at = LOCALTIMESTAMP(0)
WHERE id = '${admin_role_id}'::uuid
  AND (name IS DISTINCT FROM 'Administrador' OR description IS DISTINCT FROM 'Administra los campeonatos de su organización');

UPDATE roles
SET name = 'Propietario',
    description = 'Propietario de la plataforma: ve y gestiona todas las organizaciones',
    updated_at = LOCALTIMESTAMP(0)
WHERE id = '${owner_role_id}'::uuid
  AND (name IS DISTINCT FROM 'Propietario' OR description IS DISTINCT FROM 'Propietario de la plataforma: ve y gestiona todas las organizaciones');


-- 2. El propietario ya no lleva el rol Administrador (borrado lógico, como el resto de las asignaciones)
UPDATE users_roles
SET deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
WHERE id_role = '${admin_role_id}'::uuid
  AND deleted_at IS NULL
  AND id_user IN (
      SELECT owner_assignment.id_user
      FROM users_roles owner_assignment
      WHERE owner_assignment.id_role = '${owner_role_id}'::uuid AND owner_assignment.deleted_at IS NULL
  );


-- 3. Mismo menú para el propietario que para el administrador
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${owner_role_id}'::uuid, admin_menu.id_menu, admin_menu.visible, admin_menu.can_read,
       admin_menu.can_creat, admin_menu.can_update, admin_menu.can_delete, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM roles_menu admin_menu
WHERE admin_menu.id_role = '${admin_role_id}'::uuid
  AND admin_menu.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu owner_menu
      WHERE owner_menu.id_role = '${owner_role_id}'::uuid
        AND owner_menu.id_menu = admin_menu.id_menu
        AND owner_menu.deleted_at IS NULL
  );
