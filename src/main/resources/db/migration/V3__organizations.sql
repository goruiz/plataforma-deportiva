-- =====================================================================================================================
-- V3 · Organizaciones: cada cliente de la plataforma solo ve lo suyo
--
-- Una organización es quien adquiere el sistema para gestionar sus propios campeonatos (una liga barrial, un estadio,
-- un complejo). Lo que ella crea le pertenece y ninguna otra organización lo ve; el dueño de la plataforma (el rol
-- PLATFORM_OWNER) ve todo. Un registro sin organización pertenece al dueño: así quedan todos los datos que ya existían.
--
--   1. Tabla `organizations`.
--   2. Columna `organization_id` (con clave foránea e índice) en lo que tiene dueño: usuarios, eventos, equipos,
--      canchas, complejos, categorías, feriados y formatos de evento. En los formatos, sin organización significa
--      «predefinido»: lo usan todas las organizaciones y solo el dueño lo cambia. Partidos, inscripciones, jugadores y
--      demás cuelgan de uno de esos y heredan su organización, por eso no llevan columna propia.
--   3. El nombre de un equipo pasa a ser único dentro de su organización, no en todo el sistema.
--   4. Rol del dueño de la plataforma, que se entrega a los administradores que ya existían (hasta hoy veían todo).
--
-- Es idempotente (IF NOT EXISTS, ON CONFLICT, NOT EXISTS), como las anteriores.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol del dueño de la plataforma; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


-- 1. Organizaciones
CREATE TABLE IF NOT EXISTS organizations (
    id           UUID PRIMARY KEY,
    name         VARCHAR(150) NOT NULL,
    description  VARCHAR(500),
    status       VARCHAR(20) NOT NULL,
    created_by   UUID,
    created_at   TIMESTAMP(6) NOT NULL,
    updated_by   UUID,
    updated_at   TIMESTAMP(6),
    deleted_by   UUID,
    deleted_at   TIMESTAMP(6)
);


-- 2. Dueño de cada registro
ALTER TABLE users            ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE events           ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE teams            ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE courts           ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE sport_complexes  ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE categories       ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE holidays         ADD COLUMN IF NOT EXISTS organization_id UUID;
ALTER TABLE events_formats   ADD COLUMN IF NOT EXISTS organization_id UUID;

-- Clave foránea e índice de cada una. El índice sirve a todas las consultas, que filtran por organización.
DO $$
DECLARE
    owned_table TEXT;
BEGIN
    FOREACH owned_table IN ARRAY ARRAY['users', 'events', 'teams', 'courts', 'sport_complexes', 'categories',
                                       'holidays', 'events_formats']
    LOOP
        IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_' || owned_table || '_organization') THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (organization_id) REFERENCES organizations (id)',
                           owned_table, 'fk_' || owned_table || '_organization');
        END IF;
        EXECUTE format('CREATE INDEX IF NOT EXISTS %I ON %I (organization_id)',
                       'idx_' || owned_table || '_organization', owned_table);
    END LOOP;
END $$;


-- 3. El nombre de un equipo es único dentro de su organización. La restricción antigua (sobre `name` solo, con el nombre
--    que le dio la base) se busca en vez de nombrarla, porque una base creada por Hibernate pudo ponerle otro. El índice
--    nuevo trata «sin organización» como una más, porque NULL nunca es igual a NULL en un índice único.
DO $$
DECLARE
    old_constraint RECORD;
BEGIN
    FOR old_constraint IN
        SELECT con.conname
        FROM pg_constraint con
        JOIN pg_class rel ON rel.oid = con.conrelid
        WHERE rel.relname = 'teams'
          AND rel.relnamespace = current_schema()::regnamespace
          AND con.contype = 'u'
          AND array_length(con.conkey, 1) = 1
          AND (SELECT att.attname FROM pg_attribute att
               WHERE att.attrelid = con.conrelid AND att.attnum = con.conkey[1]) = 'name'
    LOOP
        EXECUTE format('ALTER TABLE teams DROP CONSTRAINT %I', old_constraint.conname);
    END LOOP;
END $$;

CREATE UNIQUE INDEX IF NOT EXISTS ux_teams_organization_name
    ON teams (COALESCE(organization_id, '00000000-0000-0000-0000-000000000000'::uuid), name);


-- 4. Rol del dueño de la plataforma. Su id es app.security.owner-role-id: se comprueba por id, nunca por nombre. El dueño
--    también tiene el rol administrador, que es lo que le da entrada al panel.
INSERT INTO roles (id, name, created_at, updated_at)
VALUES ('${owner_role_id}'::uuid, 'PLATFORM_OWNER', LOCALTIMESTAMP(0), LOCALTIMESTAMP(0))
ON CONFLICT (id) DO NOTHING;

-- Hasta ahora todo administrador veía todo: sigue siéndolo, ahora como dueño. Las organizaciones que se creen desde aquí
-- tendrán sus propios administradores, sin este rol.
INSERT INTO users_roles (id, id_user, id_role, created_at, updated_at)
SELECT gen_random_uuid(), ur.id_user, '${owner_role_id}'::uuid, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM users_roles ur
JOIN users u ON u.id = ur.id_user AND u.deleted_at IS NULL
WHERE ur.id_role = '${admin_role_id}'::uuid
  AND ur.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM users_roles owned
      WHERE owned.id_user = ur.id_user AND owned.id_role = '${owner_role_id}'::uuid AND owned.deleted_at IS NULL
  );
