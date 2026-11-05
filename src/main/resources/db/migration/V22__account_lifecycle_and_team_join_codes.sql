-- =====================================================================================================================
-- V22 · Ciclo de vida de la cuenta y código de equipo
--
--   1. Verificación de correo: `users.email_verified_at` (nulo = sin verificar) y la tabla de sus tokens, calcada de
--      `password_reset_tokens`. Las cuentas que ya existían quedan verificadas: no se les puede exigir un correo que
--      hasta hoy nadie les pidió confirmar.
--   2. Cierre de sesión real: `users.tokens_valid_after`. Un JWT emitido antes de esa fecha deja de valer, así que
--      cerrar sesión o eliminar la cuenta invalida el token en el servidor y no solo en el teléfono.
--   3. Código de equipo: `teams.join_code`. El administrador lo comparte y el jugador lo escribe en la app para
--      entrar al equipo sin esperar una invitación. Es opcional (nulo = el equipo no admite códigos) y único.
--
-- Es idempotente: reaplicarla no cambia nada.
-- =====================================================================================================================


-- 1. Verificación de correo
ALTER TABLE users ADD COLUMN IF NOT EXISTS email_verified_at TIMESTAMP(6);

-- Solo se verifican las cuentas que existen al aplicarla: las nuevas nacen sin verificar
UPDATE users
SET email_verified_at = COALESCE(created_at, LOCALTIMESTAMP(0))
WHERE email_verified_at IS NULL
  AND deleted_at IS NULL;

CREATE TABLE IF NOT EXISTS email_verification_tokens (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    token_hash  VARCHAR(64) NOT NULL UNIQUE,
    expires_at  TIMESTAMP NOT NULL,
    used_at     TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_user_id ON email_verification_tokens (user_id);
CREATE INDEX IF NOT EXISTS idx_email_verification_tokens_expires_at ON email_verification_tokens (expires_at);

-- Una cuenta tiene como mucho un enlace sin usar: pedir otro reemplaza al anterior
CREATE UNIQUE INDEX IF NOT EXISTS ux_email_verification_tokens_user_unused
    ON email_verification_tokens (user_id)
    WHERE used_at IS NULL;


-- 2. Cierre de sesión real
ALTER TABLE users ADD COLUMN IF NOT EXISTS tokens_valid_after TIMESTAMP(6);


-- 3. Código de equipo
ALTER TABLE teams ADD COLUMN IF NOT EXISTS join_code VARCHAR(12);

-- Parcial: solo los equipos con código entran al índice, y un equipo borrado libera el suyo
CREATE UNIQUE INDEX IF NOT EXISTS ux_teams_join_code
    ON teams (join_code)
    WHERE join_code IS NOT NULL AND deleted_at IS NULL;
