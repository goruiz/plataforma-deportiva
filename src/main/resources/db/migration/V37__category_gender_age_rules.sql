-- =====================================================================================================================
-- V37 · Reglas de género y edad por categoría, y género y fecha de nacimiento de cada persona
--
--   1. `categories`: el género que admite (`gender`: MALE o FEMALE; nulo = cualquiera) y la edad mínima y máxima en
--      años cumplidos (`min_age`, `max_age`; nulos = sin límite). Una categoría sin ninguna regla admite a todos, como
--      hasta ahora.
--   2. `users` y `players`: el género (`gender`) y la fecha de nacimiento (`birth_date`) de la persona. Los dos lados
--      guardan lo mismo (cuenta y jugador son la misma persona, como el nombre): la cuenta es lo que la app edita y el
--      jugador es lo que entra a un equipo. Son nulos hasta que la persona completa su perfil.
--
-- El género es texto libre validado por el servicio (`Gender`), como todos los estados del proyecto. Es idempotente.
-- =====================================================================================================================


ALTER TABLE categories ADD COLUMN IF NOT EXISTS gender  VARCHAR(10);
ALTER TABLE categories ADD COLUMN IF NOT EXISTS min_age INTEGER;
ALTER TABLE categories ADD COLUMN IF NOT EXISTS max_age INTEGER;

ALTER TABLE users ADD COLUMN IF NOT EXISTS gender     VARCHAR(10);
ALTER TABLE users ADD COLUMN IF NOT EXISTS birth_date DATE;

ALTER TABLE players ADD COLUMN IF NOT EXISTS gender     VARCHAR(10);
ALTER TABLE players ADD COLUMN IF NOT EXISTS birth_date DATE;
