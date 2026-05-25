-- Agrega la columna translation_key a la tabla menu.
-- Esta columna almacena la clave de traducción (ej. "menu.home") que el frontend
-- usa con easy_localization para mostrar el nombre del menú en el idioma del usuario.
-- Cuando es NULL, el frontend usa el campo name como fallback.
ALTER TABLE menu ADD COLUMN IF NOT EXISTS translation_key varchar(255);

-- Pobla los ítems de menú existentes con sus claves de traducción
UPDATE menu SET translation_key = 'menu.home'        WHERE icon = 'home';
UPDATE menu SET translation_key = 'menu.rankings'    WHERE icon = 'leaderboard';
UPDATE menu SET translation_key = 'menu.profile'     WHERE icon = 'person';
UPDATE menu SET translation_key = 'menu.leagues'     WHERE icon = 'military_tech';
UPDATE menu SET translation_key = 'menu.tournaments' WHERE icon = 'emoji_events';
