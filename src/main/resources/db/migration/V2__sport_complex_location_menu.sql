-- Ubicación exacta de los complejos deportivos (las coordenadas se eligen en el mapa del panel de administración) y su
-- pantalla en el menú. `sport_complexes.location` sigue siendo la dirección o referencia en texto.

ALTER TABLE sport_complexes
    ADD COLUMN IF NOT EXISTS latitude  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS longitude DOUBLE PRECISION;

-- Las dos coordenadas van juntas (o ninguna) y dentro del rango del globo. Cada rama pide sus columnas con IS NOT NULL
-- porque un CHECK que da NULL se acepta: sin eso, una latitud sin longitud pasaría.
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM pg_constraint WHERE conname = 'chk_sport_complexes_coordinates') THEN
        ALTER TABLE sport_complexes
            ADD CONSTRAINT chk_sport_complexes_coordinates CHECK (
                (latitude IS NULL AND longitude IS NULL)
                OR (latitude IS NOT NULL AND longitude IS NOT NULL
                    AND latitude BETWEEN -90 AND 90 AND longitude BETWEEN -180 AND 180)
            );
    END IF;
END $$;

-- «Complejos deportivos» va justo después de «Canchas»; «Feriados» se corre un lugar, y solo si sigue donde la V1 lo
-- dejó (un orden cambiado a mano no se toca).
UPDATE menu
SET nav_order = 8, "order" = 8
WHERE name = 'Feriados' AND deleted_at IS NULL AND nav_order = 7 AND "order" = 7;

INSERT INTO menu (id, name, description, name_translation_key, description_translation_key, icon, url,
                  nav_order, "order", is_active, created_at, updated_at)
SELECT gen_random_uuid(), 'Complejos deportivos', 'Gestión de los complejos deportivos, su ubicación en el mapa y sus canchas',
       'menu.admin.sport_complexes', 'menu.admin.sport_complexes_description', 'cilMap', '/sport-complexes',
       7, 7, true, LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
WHERE NOT EXISTS (SELECT 1 FROM menu m WHERE m.name = 'Complejos deportivos' AND m.deleted_at IS NULL);

-- Como el resto de pantallas del panel, solo el rol administrador la ve y puede leer, crear, editar y borrar en ella.
INSERT INTO roles_menu (id, id_role, id_menu, visible, can_read, can_creat, can_update, can_delete,
                        created_at, updated_at)
SELECT gen_random_uuid(), '${admin_role_id}'::uuid, m.id, true, true, true, true, true,
       LOCALTIMESTAMP(0), LOCALTIMESTAMP(0)
FROM menu m
WHERE m.name = 'Complejos deportivos'
  AND m.deleted_at IS NULL
  AND NOT EXISTS (
      SELECT 1 FROM roles_menu rm
      WHERE rm.id_role = '${admin_role_id}'::uuid AND rm.id_menu = m.id AND rm.deleted_at IS NULL
  );
