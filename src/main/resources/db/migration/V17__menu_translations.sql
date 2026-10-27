-- =====================================================================================================================
-- V17 · Textos del menú: nombres únicos y claves de traducción completas
--
-- Cada opción del menú se lee como `name_translation_key ?? name` (y su descripción como `description_translation_key ??
-- description`) en los dos clientes, cada uno con su propio diccionario; el texto de la base solo se ve cuando la clave
-- no existe. Una revisión de las filas encontró:
--
--   · Dos opciones llamadas «Liga» (/ligas y /ranking), que en el panel y en la app se leían igual, y «Torneo» en
--     singular junto a «Ligas»: sus claves (`events.event_league.name`, `events.event_tournament.name`) son las de los tipos
--     de evento y daban «Tournament» en alemán. Pasan a claves propias del menú, con nombres que no se repiten:
--     «Ligas», «Ranking de la liga» y «Torneos».
--   · «Feriados», cuando la pantalla y el diccionario dicen «Descansos/Feriados» (la V1 y la V2 lo buscan por ese nombre,
--     pero solo corren al crear la base: aquí ya se puede renombrar).
--   · Descripciones sin clave («Menú de liga», «Ranking», «Menú de torneo») y opciones sin descripción. La app las
--     muestra bajo cada opción y las leía sin traducir; ahora cada una lleva su clave y la app la resuelve.
--
-- Cada cliente redacta la misma clave para su público (el panel habla de gestionar, la app de «tu equipo»), así que el
-- texto de la base es el del panel, que es el que se ve si falta la clave. Las claves nuevas están en los diccionarios del
-- panel (src/assets/i18n) y de la app (assets/translations), en los cuatro idiomas.
--
-- Las opciones se buscan por url, nunca por id. Es idempotente: solo toca lo que difiere.
-- =====================================================================================================================


UPDATE menu m
SET name = text.name,
    name_translation_key = text.name_key,
    description = text.description,
    description_translation_key = text.description_key,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/home',     'Inicio',             'menu.home',                'Pantalla de inicio de la app móvil',                        'menu.home_description'),
        ('/ligas',    'Ligas',              'menu.leagues.title',       'Las ligas de la competición, de solo lectura',              'menu.leagues.description'),
        ('/ranking',  'Ranking de la liga', 'menu.league_ranking',      'Opción de la app móvil, aún sin pantalla',                  'menu.league_ranking_description'),
        ('/torneo',   'Torneos',            'menu.tournaments.title',   'Opción de la app móvil, aún sin pantalla',                  'menu.tournaments.description'),
        ('/matches',  'Partidos',           'home.action_my_matches',   'Todos los partidos de la competición, de solo lectura',     'menu.matches_description'),
        ('/rankings', 'Rankings',           'menu.rankings',            'Posiciones y líderes de cada liga, de solo lectura',        'menu.rankings_description'),
        ('/reports',  'Reportes',           'menu.reports',             'Resumen de eventos, equipos, partidos y goles, de solo lectura', 'menu.reports_description'),
        ('/profile',  'Perfil',             'menu.profile',             'Los datos de la cuenta y el cambio de contraseña',          'menu.profile_description')
) AS text (url, name, name_key, description, description_key)
WHERE m.url = text.url
  AND m.deleted_at IS NULL
  AND (m.name IS DISTINCT FROM text.name
       OR m.name_translation_key IS DISTINCT FROM text.name_key
       OR m.description IS DISTINCT FROM text.description
       OR m.description_translation_key IS DISTINCT FROM text.description_key);


-- La pantalla y el diccionario dicen «Descansos/Feriados»
UPDATE menu
SET name = 'Descansos/Feriados',
    updated_at = LOCALTIMESTAMP(0)
WHERE url = '/holidays'
  AND deleted_at IS NULL
  AND name IS DISTINCT FROM 'Descansos/Feriados';
