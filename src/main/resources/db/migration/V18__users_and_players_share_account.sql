-- =====================================================================================================================
-- V18 · Una cuenta y un jugador son la misma persona
--
-- Quien juega en la app es una cuenta (`users`, para iniciar sesión) y un jugador (`players`, para estar en un equipo)
-- con el mismo correo: las invitaciones, los partidos propios y la búsqueda del panel encuentran al jugador por él.
-- Hasta ahora ninguna de las dos formas de registrarse creaba las dos cosas:
--   · el registro abierto creaba solo la cuenta, y esa persona no podía estar en un equipo;
--   · el registro con invitación creaba solo el jugador, y esa persona no podía iniciar sesión.
-- Desde esta versión el backend crea las dos a la vez; esta migración arregla lo que ya existe:
--   1. La contraseña deja de guardarse en el jugador (vive en la cuenta): `players.password_hash` ya no es obligatoria.
--   2. Los correos de los jugadores quedan en minúsculas, como el login los lee (salvo que choquen con otro jugador).
--   3. Los jugadores sin cuenta reciben una, con la contraseña que eligieron al registrarse.
--   4. Las cuentas que no tienen ningún rol pasan a ser «Usuario» (el mismo paso de V13), incluidas las del punto 3.
--   5. Las cuentas de la app sin jugador reciben uno. Las del panel (Propietario y Administrador) no son jugadores.
--   6. Se borran las contraseñas que los jugadores guardaban.
--
-- Es idempotente (NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}        UUID del rol «Administrador»; es el mismo app.security.admin-role-id.
--   ${owner_role_id}        UUID del rol «Propietario»; es el mismo app.security.owner-role-id.
--   ${mobile_user_role_id}  UUID del rol «Usuario»; es el mismo app.security.mobile-user-role-id.
-- =====================================================================================================================


-- 1. La contraseña vive en la cuenta
ALTER TABLE players ALTER COLUMN password_hash DROP NOT NULL;


-- 2. Correos en minúsculas. Si dos jugadores solo se diferenciaban en las mayúsculas, solo cambia uno (el de menor id)
--    para no romper la restricción de correo único.
UPDATE players p
SET email = lower(p.email),
    updated_at = LOCALTIMESTAMP(0)
WHERE p.email <> lower(p.email)
  AND NOT EXISTS (
      SELECT 1 FROM players other
      WHERE other.id <> p.id
        AND lower(other.email) = lower(p.email)
        AND (other.email = lower(other.email) OR other.id < p.id)
  );


-- 3. Los jugadores registrados con una invitación reciben su cuenta, con la contraseña que eligieron
INSERT INTO users (id, first_name, last_name, email, password, created_at, updated_at)
SELECT DISTINCT ON (lower(p.email))
       gen_random_uuid(), p.first_name, p.last_name, lower(p.email), p.password_hash,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM players p
WHERE p.deleted_at IS NULL
  AND p.password_hash IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM users u
      WHERE lower(u.email) = lower(p.email)
        AND u.deleted_at IS NULL
  )
ORDER BY lower(p.email), p.created_at;


-- 4. Las cuentas sin ningún rol pasan a ser usuarios (el menú de la app se concede por rol)
INSERT INTO users_roles (id, id_user, id_role, created_at, updated_at)
SELECT gen_random_uuid(), u.id, '${mobile_user_role_id}'::uuid, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM users u
WHERE u.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM users_roles ur
      WHERE ur.id_user = u.id AND ur.deleted_at IS NULL
  );


-- 5. Las cuentas de la app sin jugador reciben uno. Mientras no completen su perfil, el jugador usa la parte del correo
--    anterior a la @ como nombre (un jugador siempre tiene nombre). Un jugador borrado que tenía ese correo sigue
--    ocupándolo, así que esa cuenta se omite.
INSERT INTO players (id, email, first_name, last_name, status, created_at, updated_at)
SELECT DISTINCT ON (lower(u.email))
       gen_random_uuid(),
       lower(u.email),
       left(COALESCE(NULLIF(btrim(u.first_name), ''), split_part(lower(u.email), '@', 1)), 100),
       left(COALESCE(btrim(u.last_name), ''), 100),
       'ACTIVE',
       LOCALTIMESTAMP(0),
       LOCALTIMESTAMP(0)
FROM users u
WHERE u.deleted_at IS NULL
  AND u.email IS NOT NULL
  AND NOT EXISTS (
      SELECT 1 FROM users_roles ur
      WHERE ur.id_user = u.id
        AND ur.deleted_at IS NULL
        AND ur.id_role IN ('${admin_role_id}'::uuid, '${owner_role_id}'::uuid)
  )
  AND NOT EXISTS (
      SELECT 1 FROM players p
      WHERE lower(p.email) = lower(u.email)
  )
ORDER BY lower(u.email), u.created_at;


-- 6. Las contraseñas ya viven en `users`
UPDATE players
SET password_hash = NULL
WHERE password_hash IS NOT NULL;
