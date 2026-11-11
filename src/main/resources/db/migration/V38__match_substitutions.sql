-- =====================================================================================================================
-- V38 · Cambios de jugadores en un partido
--
--   Un cambio es una línea más de la cronología del partido (`match_events.event_type = 'SUBSTITUTION'`): `player_id`
--   es quien entra y `replaced_player_id` (nuevo, nulo para las demás líneas) quien sale. La regla «número de cambios»
--   de la configuración (`system_settings.max_substitutions`) limita cuántos puede hacer cada equipo en un partido.
--
-- Es idempotente.
-- =====================================================================================================================


ALTER TABLE match_events ADD COLUMN IF NOT EXISTS replaced_player_id UUID;
