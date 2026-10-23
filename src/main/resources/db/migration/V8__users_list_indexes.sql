-- =====================================================================================================================
-- V8 · Lista de usuarios: índices para que siga siendo rápida con muchísimas cuentas
--
-- La pantalla de usuarios pide una página a la vez (orden por nombre, filtro por rol y búsqueda por texto) y cuenta
-- cuántas cuentas tiene cada pestaña. Sin índices cada una de esas consultas recorre toda la tabla. Aquí se crean:
--
--   1. `users_search_text(...)`: el nombre, usuario y correo en minúsculas dentro de un solo texto. La búsqueda de la
--      API pregunta por esa función (una palabra = un LIKE), así un único índice sirve a todas las palabras.
--   2. El índice del orden de la lista (nombre, apellido, correo, id) para que las primeras páginas salgan directo de él.
--   3. Los de roles: quién tiene cierto rol (pestañas Propietarios y Administradores) y qué roles tiene cada cuenta
--      de la página.
--   4. Correo y usuario: los consulta el login y el alta de usuarios, y no tenían índice.
--   5. El índice de trigramas (pg_trgm) de la búsqueda. La extensión se crea solo si el usuario de la base puede;
--      si no, la búsqueda funciona igual pero sin ese índice.
--
-- Es idempotente (IF NOT EXISTS / OR REPLACE), como las anteriores. Los índices se crean sin CONCURRENTLY porque Flyway
-- migra dentro de una transacción: con una tabla enorme conviene aplicarla en una ventana de poco tráfico.
-- =====================================================================================================================


-- 1. Texto de búsqueda de una cuenta (inmutable, para poder indexarlo)
CREATE OR REPLACE FUNCTION users_search_text(
    p_first_name       VARCHAR,
    p_middle_name      VARCHAR,
    p_last_name        VARCHAR,
    p_second_last_name VARCHAR,
    p_username         VARCHAR,
    p_email            VARCHAR
) RETURNS TEXT
    LANGUAGE SQL
    IMMUTABLE
    PARALLEL SAFE
AS $$
    SELECT lower(coalesce(p_first_name, '') || ' ' || coalesce(p_middle_name, '') || ' ' || coalesce(p_last_name, '')
                 || ' ' || coalesce(p_second_last_name, '') || ' ' || coalesce(p_username, '') || ' ' || coalesce(p_email, ''))
$$;


-- 2. Orden de la lista: la misma clave que usa la API (nombre, apellido, correo, id), solo de las cuentas activas
CREATE INDEX IF NOT EXISTS ix_users_active_list_order
    ON users (first_name, last_name, email, id)
    WHERE deleted_at IS NULL;


-- 3. Roles: quién tiene un rol, y los roles de unas cuentas concretas
CREATE INDEX IF NOT EXISTS ix_users_roles_role_user
    ON users_roles (id_role, id_user)
    WHERE deleted_at IS NULL;

CREATE INDEX IF NOT EXISTS ix_users_roles_user
    ON users_roles (id_user)
    WHERE deleted_at IS NULL;


-- 4. Búsquedas exactas por correo y por usuario
CREATE INDEX IF NOT EXISTS ix_users_email ON users (email);
CREATE INDEX IF NOT EXISTS ix_users_username ON users (username);


-- 5. Búsqueda de texto: índice de trigramas, si la extensión está disponible
DO $$
BEGIN
    CREATE EXTENSION IF NOT EXISTS pg_trgm;

    CREATE INDEX IF NOT EXISTS ix_users_search_text_trgm
        ON users USING gin (users_search_text(first_name, middle_name, last_name, second_last_name, username, email) gin_trgm_ops)
        WHERE deleted_at IS NULL;
EXCEPTION
    WHEN insufficient_privilege OR undefined_file OR feature_not_supported THEN
        RAISE NOTICE 'pg_trgm is not available: the users search works, but without its index';
END
$$;
