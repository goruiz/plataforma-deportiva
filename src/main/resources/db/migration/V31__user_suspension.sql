-- =====================================================================================================================
-- V31 · Suspender cuentas de usuario
--
--   Hasta ahora una cuenta solo podía borrarse (físicamente, sin vuelta atrás). Una cuenta suspendida conserva todo lo
--   suyo (roles, equipo, historial) pero no puede iniciar sesión ni conserva las sesiones que tenía abiertas.
--
--   · `users.suspended_at`: el momento en que se suspendió; null = la cuenta está activa. Reactivarla lo vuelve a null.
--   · Cerrar las sesiones abiertas no necesita otra columna: suspender también adelanta `tokens_valid_after`, la misma que
--     usa «cerrar sesión», y la consulta de sesión del filtro JWT ignora a quien tenga `suspended_at`.
--
-- Es idempotente (IF NOT EXISTS), como las anteriores.
-- =====================================================================================================================


ALTER TABLE users ADD COLUMN IF NOT EXISTS suspended_at TIMESTAMP(6);
