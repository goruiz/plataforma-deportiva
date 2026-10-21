-- =====================================================================================================================
-- V1 · Migración inicial: esquema completo y datos iniciales de la plataforma deportiva
--
-- Única migración del backend: reemplaza a las antiguas V1–V7 y a los seeders de Java (usuarios, menú y formatos de
-- evento). Crea todas las tablas y siembra los datos de referencia. Cualquier cambio posterior va en una nueva
-- V{n}__descripcion.sql, nunca editando esta.
--
-- Es idempotente (IF NOT EXISTS, ON CONFLICT, NOT EXISTS): sobre una base que ya tenga tablas (creadas antes por
-- Hibernate) Flyway la toma como línea base 0 y este script solo completa lo que falte.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}    UUID del rol administrador; es el mismo app.security.admin-role-id.
--   ${seed_demo_users}  'true' crea las cuentas demo; 'false' en producción (perfil prd).
-- =====================================================================================================================


-- =====================================================================================================================
-- 1. Tablas
--    Todas llevan auditoría (created_*, updated_*, deleted_*); deleted_at distinto de NULL es un borrado lógico.
-- =====================================================================================================================

-- Usuarios, roles y menú de navegación
CREATE TABLE IF NOT EXISTS users (
    id                UUID PRIMARY KEY,
    first_name        VARCHAR(255),
    middle_name       VARCHAR(255),
    last_name         VARCHAR(255),
    second_last_name  VARCHAR(255),
    username          VARCHAR(255),
    email             VARCHAR(255),
    password          VARCHAR(255),
    created_by        UUID,
    created_at        TIMESTAMP(6) NOT NULL,
    updated_by        UUID,
    updated_at        TIMESTAMP(6),
    deleted_by        UUID,
    deleted_at        TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS roles (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(255),
    description                  VARCHAR(255),
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS users_roles (
    id          UUID PRIMARY KEY,
    id_role     UUID,
    id_user     UUID,
    created_by  UUID,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMP(6),
    deleted_by  UUID,
    deleted_at  TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS permissions (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(255),
    description                  VARCHAR(255),
    id_role                      UUID,
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS menu (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(255),
    description                  VARCHAR(255),
    id_parent_menu               UUID,
    "order"                      INTEGER,
    url                          VARCHAR(255),
    icon                         VARCHAR(255),
    is_active                    BOOLEAN,
    nav_order                    SMALLINT,
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS roles_menu (
    id          UUID PRIMARY KEY,
    visible     BOOLEAN,
    can_read    BOOLEAN,
    can_creat   BOOLEAN,
    can_update  BOOLEAN,
    can_delete  BOOLEAN,
    id_menu     UUID,
    id_role     UUID,
    created_by  UUID,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMP(6),
    deleted_by  UUID,
    deleted_at  TIMESTAMP(6)
);

-- Catálogos: categorías, tipos y formatos de evento, canchas y complejos deportivos, feriados, acciones de partido
CREATE TABLE IF NOT EXISTS categories (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(100) NOT NULL,
    description                  VARCHAR(255),
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS events_types (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(100) NOT NULL,
    description                  VARCHAR(255),
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS events_formats (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(100) NOT NULL,
    description                  VARCHAR(255),
    structure                    VARCHAR(30),
    round_trip                   BOOLEAN,
    points_for_win               INTEGER,
    points_for_draw              INTEGER,
    points_for_loss              INTEGER,
    tiebreakers                  VARCHAR(255),
    swiss_rounds                 INTEGER,
    group_count                  INTEGER,
    teams_advancing_per_group    INTEGER,
    playoff_teams                INTEGER,
    knockout_round_trip          BOOLEAN,
    third_place_match            BOOLEAN,
    grand_final_reset            BOOLEAN,
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS sport_complexes (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(200) NOT NULL,
    description                  VARCHAR(500),
    location                     VARCHAR(500),
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS courts (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(150) NOT NULL,
    description                  VARCHAR(255),
    location                     VARCHAR(255),
    id_sport_complex             UUID,
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS holidays (
    id                UUID PRIMARY KEY,
    name              VARCHAR(100) NOT NULL,
    holiday_date      DATE NOT NULL,
    holiday_end_date  DATE,
    recurring         BOOLEAN NOT NULL,
    created_by        UUID,
    created_at        TIMESTAMP(6) NOT NULL,
    updated_by        UUID,
    updated_at        TIMESTAMP(6),
    deleted_by        UUID,
    deleted_at        TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS actions (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(255),
    description                  VARCHAR(255),
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

-- Equipos, jugadores e invitaciones
CREATE TABLE IF NOT EXISTS teams (
    id           UUID PRIMARY KEY,
    name         VARCHAR(150) NOT NULL UNIQUE,
    logo_url     VARCHAR(500),
    category_id  UUID,
    status       VARCHAR(20) NOT NULL,
    created_by   UUID,
    created_at   TIMESTAMP(6) NOT NULL,
    updated_by   UUID,
    updated_at   TIMESTAMP(6),
    deleted_by   UUID,
    deleted_at   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS players (
    id                 UUID PRIMARY KEY,
    email              VARCHAR(255) NOT NULL UNIQUE,
    password_hash      VARCHAR(255) NOT NULL,
    first_name         VARCHAR(100) NOT NULL,
    last_name          VARCHAR(100) NOT NULL,
    phone              VARCHAR(20),
    profile_photo_url  VARCHAR(500),
    status             VARCHAR(20) NOT NULL,
    id_team            UUID,
    created_by         UUID,
    created_at         TIMESTAMP(6) NOT NULL,
    updated_by         UUID,
    updated_at         TIMESTAMP(6),
    deleted_by         UUID,
    deleted_at         TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS team_invitations (
    id          UUID PRIMARY KEY,
    email       VARCHAR(255) NOT NULL,
    team_id     UUID NOT NULL,
    token       VARCHAR(500) UNIQUE,
    type        VARCHAR(20) NOT NULL,
    status      VARCHAR(20) NOT NULL,
    expires_at  TIMESTAMP(6) NOT NULL,
    created_by  UUID,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMP(6),
    deleted_by  UUID,
    deleted_at  TIMESTAMP(6)
);

-- Eventos, calendario y partidos
CREATE TABLE IF NOT EXISTS events (
    id                           UUID PRIMARY KEY,
    name                         VARCHAR(200),
    description                  VARCHAR(255),
    status                       VARCHAR(20),
    start_date                   DATE,
    end_date                     DATE,
    url_logo                     VARCHAR(255),
    min_players_per_team         INTEGER,
    max_players_per_team         INTEGER,
    min_teams                    INTEGER,
    max_teams                    INTEGER,
    id_event_format              UUID,
    id_event_type                UUID,
    description_translation_key  VARCHAR(255),
    name_translation_key         VARCHAR(255),
    created_by                   UUID,
    created_at                   TIMESTAMP(6) NOT NULL,
    updated_by                   UUID,
    updated_at                   TIMESTAMP(6),
    deleted_by                   UUID,
    deleted_at                   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS events_categories (
    id           UUID PRIMARY KEY,
    id_category  UUID,
    id_event     UUID,
    created_by   UUID,
    created_at   TIMESTAMP(6) NOT NULL,
    updated_by   UUID,
    updated_at   TIMESTAMP(6),
    deleted_by   UUID,
    deleted_at   TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS teams_events (
    id            UUID PRIMARY KEY,
    group_number  INTEGER,
    seed          INTEGER,
    event_id      UUID,
    team_id       UUID,
    created_by    UUID,
    created_at    TIMESTAMP(6) NOT NULL,
    updated_by    UUID,
    updated_at    TIMESTAMP(6),
    deleted_by    UUID,
    deleted_at    TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS event_schedule_config (
    id                             UUID PRIMARY KEY,
    event_id                       UUID NOT NULL,
    play_days                      VARCHAR(100) NOT NULL,
    start_time                     TIME(0) NOT NULL,
    match_duration_minutes         INTEGER NOT NULL,
    break_between_halves_minutes   INTEGER NOT NULL,
    break_between_matches_minutes  INTEGER NOT NULL,
    court_id                       UUID,
    blocked_dates                  TEXT,
    created_by                     UUID,
    created_at                     TIMESTAMP(6) NOT NULL,
    updated_by                     UUID,
    updated_at                     TIMESTAMP(6),
    deleted_by                     UUID,
    deleted_at                     TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS matches (
    id                 UUID PRIMARY KEY,
    home_team_id       UUID NOT NULL,
    away_team_id       UUID NOT NULL,
    match_date         TIMESTAMP(6) NOT NULL,
    location           VARCHAR(300),
    status             VARCHAR(20) NOT NULL,
    home_score         INTEGER,
    away_score         INTEGER,
    event_id           UUID,
    id_court           UUID,
    stage              VARCHAR(20),
    round_number       INTEGER,
    group_number       INTEGER,
    tie_number         INTEGER,
    leg_number         INTEGER,
    advancing_team_id  UUID,
    created_by         UUID,
    created_at         TIMESTAMP(6) NOT NULL,
    updated_by         UUID,
    updated_at         TIMESTAMP(6),
    deleted_by         UUID,
    deleted_at         TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS match_details (
    id          UUID PRIMARY KEY,
    id_player   UUID,
    id_team     UUID,
    minute      SMALLINT,
    id_action   UUID,
    id_match    UUID,
    created_by  UUID,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMP(6),
    deleted_by  UUID,
    deleted_at  TIMESTAMP(6)
);

CREATE TABLE IF NOT EXISTS match_events (
    id          UUID PRIMARY KEY,
    match_id    UUID NOT NULL,
    player_id   UUID NOT NULL,
    team_id     UUID NOT NULL,
    event_type  VARCHAR(30) NOT NULL,
    minute      INTEGER,
    notes       TEXT,
    created_by  UUID,
    created_at  TIMESTAMP(6) NOT NULL,
    updated_by  UUID,
    updated_at  TIMESTAMP(6),
    deleted_by  UUID,
    deleted_at  TIMESTAMP(6)
);


-- =====================================================================================================================
-- 2. Columnas agregadas a tablas que ya existían
--    CREATE TABLE IF NOT EXISTS no toca una tabla existente, así que aquí se completan las columnas que las bases
--    creadas con versiones anteriores del modelo aún no tienen. En una base nueva no hacen nada.
-- =====================================================================================================================
ALTER TABLE events
    ADD COLUMN IF NOT EXISTS url_logo             VARCHAR(255),
    ADD COLUMN IF NOT EXISTS min_players_per_team INTEGER,
    ADD COLUMN IF NOT EXISTS max_players_per_team INTEGER,
    ADD COLUMN IF NOT EXISTS min_teams            INTEGER,
    ADD COLUMN IF NOT EXISTS max_teams            INTEGER,
    ADD COLUMN IF NOT EXISTS id_event_format      UUID;

ALTER TABLE event_schedule_config
    ADD COLUMN IF NOT EXISTS blocked_dates TEXT;

ALTER TABLE events_formats
    ADD COLUMN IF NOT EXISTS structure                 VARCHAR(30),
    ADD COLUMN IF NOT EXISTS round_trip                BOOLEAN,
    ADD COLUMN IF NOT EXISTS points_for_win            INTEGER,
    ADD COLUMN IF NOT EXISTS points_for_draw           INTEGER,
    ADD COLUMN IF NOT EXISTS points_for_loss           INTEGER,
    ADD COLUMN IF NOT EXISTS tiebreakers               VARCHAR(255),
    ADD COLUMN IF NOT EXISTS swiss_rounds              INTEGER,
    ADD COLUMN IF NOT EXISTS group_count               INTEGER,
    ADD COLUMN IF NOT EXISTS teams_advancing_per_group INTEGER,
    ADD COLUMN IF NOT EXISTS playoff_teams             INTEGER,
    ADD COLUMN IF NOT EXISTS knockout_round_trip       BOOLEAN,
    ADD COLUMN IF NOT EXISTS third_place_match         BOOLEAN,
    ADD COLUMN IF NOT EXISTS grand_final_reset         BOOLEAN;

-- Hibernate creó este CHECK con las estructuras que existían entonces; las estructuras nuevas lo violarían.
ALTER TABLE events_formats
    DROP CONSTRAINT IF EXISTS events_formats_structure_check;

ALTER TABLE holidays
    ADD COLUMN IF NOT EXISTS holiday_end_date DATE;

ALTER TABLE teams_events
    ADD COLUMN IF NOT EXISTS group_number INTEGER,
    ADD COLUMN IF NOT EXISTS seed         INTEGER;

ALTER TABLE matches
    ADD COLUMN IF NOT EXISTS stage             VARCHAR(20),
    ADD COLUMN IF NOT EXISTS round_number      INTEGER,
    ADD COLUMN IF NOT EXISTS group_number      INTEGER,
    ADD COLUMN IF NOT EXISTS tie_number        INTEGER,
    ADD COLUMN IF NOT EXISTS leg_number        INTEGER,
    ADD COLUMN IF NOT EXISTS advancing_team_id UUID;


-- =====================================================================================================================
-- 3. Claves foráneas
--    Se agregan solo si la columna aún no tiene una hacia esa tabla, sin importar cómo se llame la que ya exista.
-- =====================================================================================================================
DO $$
DECLARE
    foreign_key RECORD;
BEGIN
    FOR foreign_key IN
        SELECT * FROM (VALUES
            ('courts', 'id_sport_complex', 'sport_complexes', 'fk_courts_sport_complexes'),
            ('event_schedule_config', 'court_id', 'courts', 'fk_event_schedule_config_court'),
            ('event_schedule_config', 'event_id', 'events', 'fk_event_schedule_config_event'),
            ('events', 'id_event_format', 'events_formats', 'fk_events_formats_events'),
            ('events', 'id_event_type', 'events_types', 'fk_events_types_events'),
            ('events_categories', 'id_category', 'categories', 'fk_categories_events_categories_1'),
            ('events_categories', 'id_event', 'events', 'fk_events_events_categories_2'),
            ('match_details', 'id_action', 'actions', 'fk_match_details_actions_2'),
            ('match_details', 'id_match', 'matches', 'fk_match_details_matches_1'),
            ('permissions', 'id_role', 'roles', 'fk_permissions_roles_1'),
            ('players', 'id_team', 'teams', 'fk_teams_players_1'),
            ('roles_menu', 'id_menu', 'menu', 'fk_roles_menu_menu_2'),
            ('roles_menu', 'id_role', 'roles', 'fk_roles_menu_roles_1'),
            ('team_invitations', 'team_id', 'teams', 'fk_teams_team_invitations'),
            ('teams_events', 'event_id', 'events', 'fk_events_teams_events_1'),
            ('teams_events', 'team_id', 'teams', 'fk_teams_teams_events_2'),
            ('users_roles', 'id_role', 'roles', 'fk_users_roles_roles_2'),
            ('users_roles', 'id_user', 'users', 'fk_users_roles_users_1')
        ) AS foreign_keys (table_name, column_name, referenced_table, constraint_name)
    LOOP
        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint c
            JOIN pg_attribute a ON a.attrelid = c.conrelid AND a.attnum = ANY (c.conkey)
            WHERE c.contype = 'f'
              AND c.conrelid = foreign_key.table_name::regclass
              AND c.confrelid = foreign_key.referenced_table::regclass
              AND a.attname = foreign_key.column_name
        ) THEN
            EXECUTE format('ALTER TABLE %I ADD CONSTRAINT %I FOREIGN KEY (%I) REFERENCES %I (id)',
                           foreign_key.table_name, foreign_key.constraint_name,
                           foreign_key.column_name, foreign_key.referenced_table);
        END IF;
    END LOOP;
END $$;


-- =====================================================================================================================
-- 4. Datos heredados
--    Solo actúan sobre bases creadas antes de esta migración; en una base nueva no encuentran nada que cambiar.
-- =====================================================================================================================

-- Los eventos guardaban su formato como texto libre en events.format. Cada texto distinto (sin distinguir mayúsculas ni
-- espacios sobrantes) pasa a ser un formato del catálogo, también el de los eventos con borrado lógico, y la columna se
-- elimina.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_schema = current_schema() AND table_name = 'events' AND column_name = 'format') THEN
        INSERT INTO events_formats (id, name, created_at)
        SELECT gen_random_uuid(), legacy.name, now()
        FROM (
            SELECT MIN(TRIM(format)) AS name
            FROM events
            WHERE format IS NOT NULL AND TRIM(format) <> ''
            GROUP BY LOWER(TRIM(format))
        ) legacy
        WHERE NOT EXISTS (
            SELECT 1 FROM events_formats existing
            WHERE existing.deleted_at IS NULL AND LOWER(existing.name) = LOWER(legacy.name)
        );

        UPDATE events
        SET id_event_format = events_formats.id
        FROM events_formats
        WHERE events.id_event_format IS NULL
          AND events.format IS NOT NULL
          AND events_formats.deleted_at IS NULL
          AND LOWER(events_formats.name) = LOWER(TRIM(events.format));
    END IF;
END $$;

ALTER TABLE events DROP COLUMN IF EXISTS format;

-- Los ítems de menú de la app móvil que se crearon sin clave de traducción la reciben según su icono; el frontend la
-- usa con easy_localization y, si es NULL, muestra el nombre.
UPDATE menu m
SET name_translation_key = k.translation_key
FROM (VALUES
        ('home',          'menu.home'),
        ('leaderboard',   'menu.rankings'),
        ('person',        'menu.profile'),
        ('military_tech', 'menu.leagues'),
        ('emoji_events',  'menu.tournaments')
) AS k (icon, translation_key)
WHERE m.icon = k.icon
  AND m.name_translation_key IS NULL;


-- =====================================================================================================================
-- 5. Datos iniciales
-- =====================================================================================================================

-- Rol administrador. Su id es app.security.admin-role-id: el login y los permisos lo comprueban por id, nunca por nombre.
INSERT INTO roles (id, name, created_at, updated_at)
VALUES ('${admin_role_id}'::uuid, 'ADMIN', LOCALTIMESTAMP(0), LOCALTIMESTAMP(0))
ON CONFLICT (id) DO NOTHING;

-- Cuentas demo, solo fuera de producción. Las contraseñas (guardadas en BCrypt) son el correo seguido de "1A",
-- por ejemplo admin@algo.com1A; el administrador recibe el rol ADMIN y el usuario demo queda sin rol.
INSERT INTO users (id, first_name, last_name, email, password, created_at, updated_at)
SELECT gen_random_uuid(), demo.first_name, demo.last_name, demo.email, demo.password,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM (VALUES
        ('Administrador', 'Principal', 'admin@algo.com',   '$2a$10$gmu/EFm0B2KWWxntuxYjjuvv.wj3l3Fqz7qFUR56myFHL0uZX1Am2'),
        ('Usuario', 'Demo', 'usuario@algo.com', '$2a$10$lnqM48db6ZtY6IuHJkOPiupt4H5iRexd0.jJ/ExKr3ADYJ4CmAeDu')
) AS demo (first_name, last_name, email, password)
WHERE '${seed_demo_users}' = 'true'
  AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = demo.email);

INSERT INTO users_roles (id, id_user, id_role, created_at, updated_at)
SELECT gen_random_uuid(), u.id, '${admin_role_id}'::uuid, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM users u
WHERE '${seed_demo_users}' = 'true'
  AND u.email = 'admin@algo.com'
  AND NOT EXISTS (
      SELECT 1 FROM users_roles ur
      WHERE ur.id_user = u.id AND ur.id_role = '${admin_role_id}'::uuid AND ur.deleted_at IS NULL
  );

-- Menú del panel de administración. Cada url necesita su ruta en el frontend Angular y cada clave de traducción su
-- texto en los cuatro idiomas de src/assets/i18n.
CREATE TEMP TABLE admin_menu_seed (
    name                        VARCHAR(255),
    description                 VARCHAR(255),
    name_translation_key        VARCHAR(255),
    description_translation_key VARCHAR(255),
    icon                        VARCHAR(255),
    url                         VARCHAR(255),
    nav_order                   SMALLINT,
    "order"                     INTEGER
) ON COMMIT DROP;

INSERT INTO admin_menu_seed VALUES
        ('Dashboard', 'Resumen general de la plataforma', 'menu.admin.dashboard', 'menu.admin.dashboard_description', 'cilSpeedometer', '/dashboard', 0, 0),
        ('Usuarios', 'Gestión de usuarios del sistema', 'menu.admin.users', 'menu.admin.users_description', 'cilPeople', '/users', 1, 1),
        ('Eventos', 'Gestión de eventos deportivos', 'menu.admin.events', 'menu.admin.events_description', 'cilCalendar', '/events', 2, 2),
        ('Formatos de evento', 'Gestión de los formatos de juego de los eventos', 'menu.admin.event_formats', 'menu.admin.event_formats_description', 'cilList', '/event-formats', 3, 3),
        ('Equipos', 'Gestión de equipos deportivos', 'menu.admin.teams', 'menu.admin.teams_description', 'cilShieldAlt', '/teams', 4, 4),
        ('Categorías', 'Gestión de las categorías de equipos y eventos', 'menu.admin.categories', 'menu.admin.categories_description', 'cilTags', '/categories', 5, 5),
        ('Canchas', 'Gestión de las canchas donde se juegan los partidos', 'menu.admin.courts', 'menu.admin.courts_description', 'cilLocationPin', '/courts', 6, 6),
        ('Feriados', 'Gestión de los feriados y los días en que no se juega', 'menu.admin.holidays', 'menu.admin.holidays_description', 'cilFlagAlt', '/holidays', 7, 7);

INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, created_at, updated_at)
SELECT gen_random_uuid(), s.name, s.description, s.name_translation_key, s.description_translation_key, s.icon, s.url,
       s.nav_order, s."order", true, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM admin_menu_seed s
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.name = s.name AND m.deleted_at IS NULL);

-- El rol administrador ve esos ítems y puede leer, crear, editar y borrar en ellos.
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${admin_role_id}'::uuid, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
JOIN admin_menu_seed s ON s.name = m.name
WHERE m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = '${admin_role_id}'::uuid AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );

-- Catálogo de formatos de evento: todas las combinaciones de estructura con sus reglas de ida y vuelta, tercer puesto
-- y revancha de la final (31 formatos). Un formato cuyo nombre ya exista (sin distinguir mayúsculas) no se toca.
INSERT INTO events_formats (id, name, description, structure, round_trip, points_for_win, points_for_draw,
                            points_for_loss, tiebreakers, swiss_rounds, group_count, teams_advancing_per_group,
                            playoff_teams, knockout_round_trip, third_place_match, grand_final_reset,
                            created_at, updated_at)
SELECT gen_random_uuid(), f.name, f.description, f.structure, f.round_trip, f.points_for_win, f.points_for_draw,
       f.points_for_loss, f.tiebreakers, f.swiss_rounds, f.group_count, f.teams_advancing_per_group,
       f.playoff_teams, f.knockout_round_trip, f.third_place_match, f.grand_final_reset,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM (VALUES
        ('Liga - solo ida', 'Todos los equipos se enfrentan una sola vez y gana quien más puntos suma.', 'ROUND_ROBIN', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
        ('Liga - ida y vuelta', 'Todos los equipos se enfrentan dos veces, una como local y otra como visitante y gana quien más puntos suma.', 'ROUND_ROBIN', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, NULL, NULL, NULL, NULL),
        ('Sistema suizo', 'Los equipos juegan 5 rondas; en cada una se enfrentan equipos con puntos parecidos, sin repetir cruces, y gana quien más puntos suma.', 'SWISS', NULL, 3, 1, 0, 'OPPONENTS_POINTS,GOAL_DIFFERENCE,GOALS_FOR', 5, NULL, NULL, NULL, NULL, NULL, NULL),
        ('Eliminación directa - partido único', 'Quien pierde queda eliminado y los ganadores avanzan de ronda hasta la final. Cada cruce se juega a partido único.', 'KNOCKOUT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, false, NULL),
        ('Eliminación directa - partido único + tercer puesto', 'Quien pierde queda eliminado y los ganadores avanzan de ronda hasta la final. Cada cruce se juega a partido único, con partido por el tercer puesto.', 'KNOCKOUT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, true, NULL),
        ('Eliminación directa - ida y vuelta', 'Quien pierde queda eliminado y los ganadores avanzan de ronda hasta la final. Cada cruce se juega a ida y vuelta.', 'KNOCKOUT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, false, NULL),
        ('Eliminación directa - ida y vuelta + tercer puesto', 'Quien pierde queda eliminado y los ganadores avanzan de ronda hasta la final. Cada cruce se juega a ida y vuelta, con partido por el tercer puesto.', 'KNOCKOUT', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, true, NULL),
        ('Doble eliminación - partido único', 'Un equipo queda eliminado al perder dos veces: quien pierde la primera pasa a la llave de perdedores. Cada cruce se juega a partido único; la final se juega una sola vez.', 'DOUBLE_ELIMINATION', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, NULL, false),
        ('Doble eliminación - partido único + final con revancha', 'Un equipo queda eliminado al perder dos veces: quien pierde la primera pasa a la llave de perdedores. Cada cruce se juega a partido único; si gana la final quien llegó por la llave de perdedores, se juega una revancha.', 'DOUBLE_ELIMINATION', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, false, NULL, true),
        ('Doble eliminación - ida y vuelta', 'Un equipo queda eliminado al perder dos veces: quien pierde la primera pasa a la llave de perdedores. Cada cruce se juega a ida y vuelta; la final se juega una sola vez.', 'DOUBLE_ELIMINATION', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, false),
        ('Doble eliminación - ida y vuelta + final con revancha', 'Un equipo queda eliminado al perder dos veces: quien pierde la primera pasa a la llave de perdedores. Cada cruce se juega a ida y vuelta; si gana la final quien llegó por la llave de perdedores, se juega una revancha.', 'DOUBLE_ELIMINATION', NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, true, NULL, true),
        ('Grupos (solo ida) + eliminatoria (partido único)', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta una sola vez; luego eliminatoria a partido único.', 'GROUPS_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, false, false, NULL),
        ('Grupos (solo ida) + eliminatoria (partido único) + tercer puesto', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta una sola vez; luego eliminatoria a partido único, con partido por el tercer puesto.', 'GROUPS_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, false, true, NULL),
        ('Grupos (solo ida) + eliminatoria (ida y vuelta)', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta una sola vez; luego eliminatoria a ida y vuelta.', 'GROUPS_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, true, false, NULL),
        ('Grupos (solo ida) + eliminatoria (ida y vuelta) + tercer puesto', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta una sola vez; luego eliminatoria a ida y vuelta, con partido por el tercer puesto.', 'GROUPS_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, true, true, NULL),
        ('Grupos (ida y vuelta) + eliminatoria (partido único)', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta dos veces, una como local y otra como visitante; luego eliminatoria a partido único.', 'GROUPS_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, false, false, NULL),
        ('Grupos (ida y vuelta) + eliminatoria (partido único) + tercer puesto', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta dos veces, una como local y otra como visitante; luego eliminatoria a partido único, con partido por el tercer puesto.', 'GROUPS_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, false, true, NULL),
        ('Grupos (ida y vuelta) + eliminatoria (ida y vuelta)', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta dos veces, una como local y otra como visitante; luego eliminatoria a ida y vuelta.', 'GROUPS_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, true, false, NULL),
        ('Grupos (ida y vuelta) + eliminatoria (ida y vuelta) + tercer puesto', 'Fase de grupos (4 grupos, clasifican 2 por grupo) donde cada pareja de equipos se enfrenta dos veces, una como local y otra como visitante; luego eliminatoria a ida y vuelta, con partido por el tercer puesto.', 'GROUPS_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, 4, 2, NULL, true, true, NULL),
        ('Liga (solo ida) + playoffs (partido único)', 'Todos los equipos se enfrentan una sola vez y los 4 mejores pasan a playoffs, que se juegan a partido único.', 'LEAGUE_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, false, false, NULL),
        ('Liga (solo ida) + playoffs (partido único) + tercer puesto', 'Todos los equipos se enfrentan una sola vez y los 4 mejores pasan a playoffs, que se juegan a partido único, con partido por el tercer puesto.', 'LEAGUE_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, false, true, NULL),
        ('Liga (solo ida) + playoffs (ida y vuelta)', 'Todos los equipos se enfrentan una sola vez y los 4 mejores pasan a playoffs, que se juegan a ida y vuelta.', 'LEAGUE_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, true, false, NULL),
        ('Liga (solo ida) + playoffs (ida y vuelta) + tercer puesto', 'Todos los equipos se enfrentan una sola vez y los 4 mejores pasan a playoffs, que se juegan a ida y vuelta, con partido por el tercer puesto.', 'LEAGUE_KNOCKOUT', false, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, true, true, NULL),
        ('Liga (ida y vuelta) + playoffs (partido único)', 'Todos los equipos se enfrentan dos veces, una como local y otra como visitante y los 4 mejores pasan a playoffs, que se juegan a partido único.', 'LEAGUE_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, false, false, NULL),
        ('Liga (ida y vuelta) + playoffs (partido único) + tercer puesto', 'Todos los equipos se enfrentan dos veces, una como local y otra como visitante y los 4 mejores pasan a playoffs, que se juegan a partido único, con partido por el tercer puesto.', 'LEAGUE_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, false, true, NULL),
        ('Liga (ida y vuelta) + playoffs (ida y vuelta)', 'Todos los equipos se enfrentan dos veces, una como local y otra como visitante y los 4 mejores pasan a playoffs, que se juegan a ida y vuelta.', 'LEAGUE_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, true, false, NULL),
        ('Liga (ida y vuelta) + playoffs (ida y vuelta) + tercer puesto', 'Todos los equipos se enfrentan dos veces, una como local y otra como visitante y los 4 mejores pasan a playoffs, que se juegan a ida y vuelta, con partido por el tercer puesto.', 'LEAGUE_KNOCKOUT', true, 3, 1, 0, 'GOAL_DIFFERENCE,GOALS_FOR', NULL, NULL, NULL, 4, true, true, NULL),
        ('Sistema suizo + playoffs (partido único)', 'Los equipos juegan 5 rondas suizas y los 4 mejores pasan a playoffs, que se juegan a partido único.', 'SWISS_KNOCKOUT', NULL, 3, 1, 0, 'OPPONENTS_POINTS,GOAL_DIFFERENCE,GOALS_FOR', 5, NULL, NULL, 4, false, false, NULL),
        ('Sistema suizo + playoffs (partido único) + tercer puesto', 'Los equipos juegan 5 rondas suizas y los 4 mejores pasan a playoffs, que se juegan a partido único, con partido por el tercer puesto.', 'SWISS_KNOCKOUT', NULL, 3, 1, 0, 'OPPONENTS_POINTS,GOAL_DIFFERENCE,GOALS_FOR', 5, NULL, NULL, 4, false, true, NULL),
        ('Sistema suizo + playoffs (ida y vuelta)', 'Los equipos juegan 5 rondas suizas y los 4 mejores pasan a playoffs, que se juegan a ida y vuelta.', 'SWISS_KNOCKOUT', NULL, 3, 1, 0, 'OPPONENTS_POINTS,GOAL_DIFFERENCE,GOALS_FOR', 5, NULL, NULL, 4, true, false, NULL),
        ('Sistema suizo + playoffs (ida y vuelta) + tercer puesto', 'Los equipos juegan 5 rondas suizas y los 4 mejores pasan a playoffs, que se juegan a ida y vuelta, con partido por el tercer puesto.', 'SWISS_KNOCKOUT', NULL, 3, 1, 0, 'OPPONENTS_POINTS,GOAL_DIFFERENCE,GOALS_FOR', 5, NULL, NULL, 4, true, true, NULL)
) AS f (name, description, structure, round_trip, points_for_win, points_for_draw, points_for_loss, tiebreakers,
        swiss_rounds, group_count, teams_advancing_per_group, playoff_teams, knockout_round_trip,
        third_place_match, grand_final_reset)
WHERE NOT EXISTS (
    SELECT 1 FROM events_formats e WHERE LOWER(e.name) = LOWER(f.name) AND e.deleted_at IS NULL
);
