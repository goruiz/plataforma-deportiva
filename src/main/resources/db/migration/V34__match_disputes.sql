-- =====================================================================================================================
-- V34 · Validación de resultados y disputas
--
--   1. `matches.result_recorded_at`: cuándo se registró el resultado actual (desde ahí corre el plazo para disputarlo).
--      Los partidos que ya tenían resultado la reciben de su última modificación.
--   2. `matches.result_validated_at` / `result_validated_by`: cuándo y quién confirmó el resultado como oficial. Es una
--      marca informativa: no cambia las tablas de posiciones. Cualquier cambio del resultado la borra.
--   3. `match_disputes`: la queja de un equipo contra el resultado de un partido, con su motivo, el resultado que
--      propone, su estado (OPEN, ACCEPTED o REJECTED) y cómo se resolvió. Quién la abrió es `created_by`; su nombre se
--      guarda aparte porque las cuentas pueden borrarse de verdad.
--   4. La opción «Disputas» (`/disputes`, la ruta del panel), en «Competición» justo después de «Configuración», para
--      los dos roles del panel, Administrador y Propietario.
--
-- El grupo se busca por nombre y la pantalla por url, nunca por id. Es idempotente: reaplicarla no cambia nada.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1 y 2. Lo que el partido recuerda de su resultado
ALTER TABLE matches ADD COLUMN IF NOT EXISTS result_recorded_at  TIMESTAMP(6);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS result_validated_at TIMESTAMP(6);
ALTER TABLE matches ADD COLUMN IF NOT EXISTS result_validated_by UUID;

UPDATE matches
SET result_recorded_at = COALESCE(updated_at, created_at)
WHERE result_recorded_at IS NULL
  AND home_score IS NOT NULL
  AND away_score IS NOT NULL;


-- 3. Las disputas
CREATE TABLE IF NOT EXISTS match_disputes (
    id                   UUID PRIMARY KEY,
    match_id             UUID NOT NULL REFERENCES matches(id) ON DELETE CASCADE,
    -- El equipo que disputa; es un UUID plano, como los equipos de un partido
    team_id              UUID NOT NULL,
    opened_by_name       VARCHAR(255),
    reason               VARCHAR(1000) NOT NULL,
    proposed_home_score  INTEGER,
    proposed_away_score  INTEGER,
    status               VARCHAR(20) NOT NULL,
    resolution_note      VARCHAR(1000),
    resolved_by          UUID,
    resolved_by_name     VARCHAR(255),
    resolved_at          TIMESTAMP(6),
    created_by           UUID,
    created_at           TIMESTAMP(6) NOT NULL,
    updated_by           UUID,
    updated_at           TIMESTAMP(6),
    deleted_by           UUID,
    deleted_at           TIMESTAMP(6)
);

CREATE INDEX IF NOT EXISTS idx_match_disputes_match_id ON match_disputes (match_id) WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS idx_match_disputes_team_id ON match_disputes (team_id) WHERE deleted_at IS NULL;

-- Una sola disputa abierta por equipo y partido; resolverla deja abrir otra
CREATE UNIQUE INDEX IF NOT EXISTS ux_match_disputes_open_per_team
    ON match_disputes (match_id, team_id)
    WHERE status = 'OPEN' AND deleted_at IS NULL;

-- La lista del panel, de la más reciente a la más antigua, y los conteos por estado
CREATE INDEX IF NOT EXISTS idx_match_disputes_status_created
    ON match_disputes (status, created_at DESC) WHERE deleted_at IS NULL;


-- 4. La opción «Disputas», después de «Configuración»
INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, owner_only, is_system, id_parent_menu, created_at, updated_at)
SELECT gen_random_uuid(), 'Disputas', 'Quejas de los equipos contra el resultado de un partido',
       'menu.admin.disputes', 'menu.admin.disputes_description', 'cilWarning', '/disputes',
       NULL, COALESCE((SELECT MAX(sibling."order") + 1 FROM menu sibling
                       WHERE sibling.id_parent_menu = competition_group.id AND sibling.deleted_at IS NULL), 0),
       true, false, false, competition_group.id, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu competition_group
WHERE competition_group.name = 'Competición'
  AND competition_group.url IS NULL
  AND competition_group.deleted_at IS NULL
  AND NOT EXISTS (SELECT 1 FROM menu m WHERE m.url = '/disputes' AND m.deleted_at IS NULL);

INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), granted.role_id, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
CROSS JOIN (VALUES ('${admin_role_id}'::uuid), ('${owner_role_id}'::uuid)) AS granted (role_id)
WHERE m.url = '/disputes'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = granted.role_id AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );
