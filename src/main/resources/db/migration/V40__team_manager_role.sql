-- =====================================================================================================================
-- V40 · Rol «Dirigente de equipo»
--
-- Quien dirige un equipo (no solo juega en él) necesita hacer algo que un usuario común no hace: avisar a los
-- organizadores de que el resultado de un partido de su equipo parece incorrecto (las «disputas»). En vez de dar ese
-- permiso a todos los capitanes, se crea un rol aparte:
--
--   1. El rol «Dirigente de equipo» (app.security.team-manager-role-id), buscado por id y nunca por nombre.
--   2. Con lo mismo que ve el rol «Usuario»: se copian sus pantallas de la app. Si más adelante se da una pantalla nueva
--      a «Usuario», hay que darla también a este rol desde «Menú por rol».
--   3. Lo extra (hoy, avisar de un problema con un resultado) no vive en el menú sino en `AccountCapability`, que el
--      backend reparte según el rol: un permiso nuevo del dirigente se agrega ahí, sin otra migración.
--
-- Nadie lo recibe solo: un administrador se lo asigna a quien corresponda desde «Usuarios». Es idempotente.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${team_manager_role_id}  UUID del rol «Dirigente de equipo»; es el mismo app.security.team-manager-role-id.
--   ${mobile_user_role_id}   UUID del rol «Usuario»; es el mismo app.security.mobile-user-role-id.
-- =====================================================================================================================


-- 1. El rol
INSERT INTO roles (id, name, description, created_at, updated_at)
VALUES ('${team_manager_role_id}'::uuid, 'Dirigente de equipo',
        'Un usuario de la app que además dirige un equipo: puede avisar de un problema con el resultado de un partido de su equipo',
        LOCALTIMESTAMP(0), LOCALTIMESTAMP(0))
ON CONFLICT (id) DO NOTHING;


-- 2. Las pantallas de la app: las mismas que ve el rol «Usuario»
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${team_manager_role_id}'::uuid, granted.id_menu, granted.visible, granted.can_read,
       granted.can_creat, granted.can_update, granted.can_delete, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM roles_menu granted
JOIN menu item ON item.id = granted.id_menu AND item.deleted_at IS NULL
WHERE granted.id_role = '${mobile_user_role_id}'::uuid
  AND granted.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu existing
      WHERE existing.id_role = '${team_manager_role_id}'::uuid
        AND existing.id_menu = granted.id_menu
        AND existing.deleted_at IS NULL
  );
