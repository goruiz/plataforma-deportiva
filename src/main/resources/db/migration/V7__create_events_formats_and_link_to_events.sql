-- Catálogo de formatos de evento (liga, eliminación directa, grupos + playoffs, ...).
-- Reemplaza el texto libre events.format por una referencia a este catálogo.
--
-- Todo el script es idempotente: Hibernate (ddl-auto=update) ya crea la tabla y la columna al
-- arrancar, así que correrlo a mano después solo migra los textos viejos y quita events.format.
CREATE TABLE IF NOT EXISTS events_formats (
    id                          UUID         NOT NULL PRIMARY KEY,
    name                        VARCHAR(100) NOT NULL,
    description                 VARCHAR(255),
    created_at                  TIMESTAMP    NOT NULL,
    created_by                  UUID,
    updated_at                  TIMESTAMP,
    updated_by                  UUID,
    deleted_at                  TIMESTAMP,
    deleted_by                  UUID,
    description_translation_key VARCHAR(255),
    name_translation_key        VARCHAR(255)
);

ALTER TABLE events ADD COLUMN IF NOT EXISTS id_event_format UUID;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'fk_events_formats_events') THEN
        ALTER TABLE events
            ADD CONSTRAINT fk_events_formats_events
                FOREIGN KEY (id_event_format) REFERENCES events_formats (id);
    END IF;
END $$;

-- Conserva lo que ya estaba escrito: cada texto distinto (sin distinguir mayúsculas ni espacios
-- sobrantes) pasa a ser un formato del catálogo. Se incluyen también los eventos borrados lógicamente.
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns
               WHERE table_name = 'events' AND column_name = 'format') THEN
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
