-- =====================================================================================================================
-- V13 · Rol «Usuario»: el tercero de los roles de la plataforma
--
-- La plataforma tiene tres roles sembrados, todos buscados por id y nunca por nombre:
--   · «Propietario»    (app.security.owner-role-id, V3/V4): el más alto; ve y gestiona todas las organizaciones y es el
--                      único que toca los ajustes globales (roles, menú, organizaciones).
--   · «Administrador»  (app.security.admin-role-id, V1/V4): quien adquirió la app y administra una liga, un torneo, un
--                      complejo...; solo ve lo de su organización.
--   · «Usuario»        (app.security.mobile-user-role-id, esta migración): quien usa la app móvil porque participa en
--                      eventos (jugadores, capitanes). Es el que el registro de la app asigna.
--
-- El menú se concede por rol y quien no tiene ninguno recibe el menú vacío. El registro de la app no asignaba rol, así que
-- ninguna cuenta registrada veía su menú. Esta migración:
--   1. Crea el rol «Usuario».
--   2. Le concede las pantallas de la app (las mismas que dieron V10 y V11 a los roles que no son del panel).
--   3. Se lo asigna a las cuentas que hoy no tienen ningún rol (las ya registradas y la cuenta demo usuario@algo.com).
--
-- Es idempotente (ON CONFLICT / NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${mobile_user_role_id}  UUID del rol «Usuario»; es el mismo app.security.mobile-user-role-id.
-- =====================================================================================================================


-- 1. El rol
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES ('${mobile_user_role_id}'::uuid, 'Usuario', 'Quienes usan la app móvil porque participan en eventos: jugadores y capitanes',
        LOCALTIMESTAMP(0), LOCALTIMESTAMP(0))
ON CONFLICT (id) DO NOTHING;


-- 2. Las pantallas de la app: solo lectura
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${mobile_user_role_id}'::uuid, item.id, true, true, false, false, false,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu item
WHERE item.url IN ('/home', '/ligas', '/teams', '/events', '/matches', '/rankings', '/reports', '/profile', '/courts')
  AND item.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu existing
      WHERE existing.id_role = '${mobile_user_role_id}'::uuid
        AND existing.id_menu = item.id
        AND existing.deleted_at IS NULL
  );


-- 3. Las cuentas sin ningún rol pasan a ser usuarios
INSERT INTO users_roles (id, id_user, id_role, created_at, updated_at)
SELECT gen_random_uuid(), u.id, '${mobile_user_role_id}'::uuid, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM users u
WHERE u.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM users_roles ur
      WHERE ur.id_user = u.id AND ur.deleted_at IS NULL
  );
