-- =====================================================================================================================
-- V33 · Configuración del sistema: las reglas de los partidos de cada organización, y los penales
--
--   1. `system_settings`: una fila por organización con sus reglas (tiempo de espera antes del partido, cambios por
--      equipo, si se permite el empate, tiempo extra, si hay penales y cuánto tiempo hay para disputar un resultado).
--      La fila SIN organización son los valores por defecto de la plataforma: la heredan todas las organizaciones que
--      aún no guardaron los suyos. Esta migración la siembra con los valores de siempre (empate permitido, sin penales).
--   2. `matches.home_penalty_score` / `away_penalty_score`: la tanda de penales que desempató un resultado empatado.
--      Nulos en todos los partidos que ya existen.
--   3. La opción «Configuración» (`/settings`, la ruta del panel), al final del grupo «Competición», para los dos roles
--      del panel, Administrador y Propietario. Qué fila escribe cada uno lo decide el backend.
--
-- El grupo se busca por nombre y la pantalla por url, nunca por id. Es idempotente: reaplicarla no cambia nada.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. Las reglas de cada organización
CREATE TABLE IF NOT EXISTS system_settings (
    id                    UUID PRIMARY KEY,
    -- Nulo: los valores por defecto de la plataforma. Sin FK: la organización se borra de forma lógica.
    organization_id       UUID,
    match_wait_minutes    INTEGER NOT NULL,
    max_substitutions     INTEGER NOT NULL,
    draws_allowed         BOOLEAN NOT NULL,
    extra_time_minutes    INTEGER NOT NULL,
    penalties_enabled     BOOLEAN NOT NULL,
    dispute_window_hours  INTEGER NOT NULL,
    created_by            UUID,
    created_at            TIMESTAMP(6) NOT NULL,
    updated_by            UUID,
    updated_at            TIMESTAMP(6),
    deleted_by            UUID,
    deleted_at            TIMESTAMP(6)
);

-- Una fila por organización; y una sola de la plataforma (un índice único no distingue los nulos entre sí)
CREATE UNIQUE INDEX IF NOT EXISTS ux_system_settings_organization
    ON system_settings (organization_id)
    WHERE organization_id IS NOT NULL AND deleted_at IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS ux_system_settings_platform
    ON system_settings ((1))
    WHERE organization_id IS NULL AND deleted_at IS NULL;

-- Los valores por defecto de la plataforma
INSERT INTO system_settings (id, organization_id, match_wait_minutes, max_substitutions, draws_allowed,
                             extra_time_minutes, penalties_enabled, dispute_window_hours, created_at, updated_at)
SELECT gen_random_uuid(), NULL, 15, 5, true, 0, false, 48, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM system_settings WHERE organization_id IS NULL AND deleted_at IS NULL);


-- 2. La tanda de penales de un partido
ALTER TABLE matches ADD COLUMN IF NOT EXISTS home_penalty_score INTEGER;
ALTER TABLE matches ADD COLUMN IF NOT EXISTS away_penalty_score INTEGER;


-- 3. La opción «Configuración», al final de «Competición»
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, id_parent_menu, created_at, updated_at)
SELECT gen_random_uuid(), 'Configuración', 'Reglas de los partidos: empates, penales, cambios y disputas',
       'menu.admin.settings', 'menu.admin.settings_description', 'cilSettings', '/settings',
       NULL, COALESCE((SELECT MAX(sibling."order") + 1 FROM menu sibling
                       WHERE sibling.id_parent_menu = competition_group.id AND sibling.deleted_at IS NULL), 0),
       true, false, false, competition_group.id, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu competition_group
WHERE competition_group.name = 'Competición'
  AND competition_group.url IS NULL
  AND competition_group.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM menu m WHERE m.url = '/settings' AND m.deleted_at IS NULL);

INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), granted.role_id, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
CROSS JOIN (VALUES ('${admin_role_id}'::uuid), ('${owner_role_id}'::uuid)) AS granted (role_id)
WHERE m.url = '/settings'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = granted.role_id AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );
