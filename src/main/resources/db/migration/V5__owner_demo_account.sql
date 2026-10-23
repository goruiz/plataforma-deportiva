-- =====================================================================================================================
-- V5 · Cuenta demo del propietario de la plataforma
--
-- Crea propietario@algo.com con el rol Propietario (app.security.owner-role-id): entra al panel de administración y ve
-- todas las organizaciones. Como las cuentas demo de la V1, solo se crea fuera de producción (${seed_demo_users} = 'true');
-- en producción (perfil prd) el primer propietario se crea aparte, con el rol Propietario.
--
-- La contraseña no está en el repositorio, solo su hash BCrypt: se entregó al crear la cuenta y se cambia desde el perfil
-- del panel (Cambiar contraseña).
--
-- Es idempotente (NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${owner_role_id}    UUID del rol Propietario; es el mismo app.security.owner-role-id.
--   ${seed_demo_users}  'true' crea las cuentas demo; 'false' en producción (perfil prd).
-- =====================================================================================================================

INSERT INTO users (id, first_name, last_name, email, username, password, created_at, updated_at)
SELECT gen_random_uuid(), 'Propietario', 'Plataforma', 'propietario@algo.com', 'propietario',
       '$2a$10$lwE5.PL5VYSKo8AeYSHVN.SWUvId4nXgIYAWSJBsa/ij25T3/rSz6', LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE '${seed_demo_users}' = 'true'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = 'propietario@algo.com');

INSERT INTO users_roles (id, id_user, id_role, created_at, updated_at)
SELECT gen_random_uuid(), u.id, '${owner_role_id}'::uuid, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM users u
WHERE '${seed_demo_users}' = 'true'
  AND u.email = 'propietario@algo.com'
  AND u.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM users_roles ur
      WHERE ur.id_user = u.id AND ur.id_role = '${owner_role_id}'::uuid AND ur.deleted_at IS NULL
  );