-- =====================================================================================================================
-- V41 · «App móvil», «Inicio» y «Perfil» no son parte del menú del propietario ni del administrador
--
-- Esas opciones son de la app móvil (rol «Usuario» y «Dirigente de equipo»): el panel no tiene pantalla «Inicio» y su
-- «Perfil» se abre desde el menú de usuario del encabezado, no desde el menú lateral. Aun así el propietario las veía,
-- porque el backend le muestra todas las opciones activas sin mirar `roles_menu`, y un administrador que además tuviera un
-- rol de la app también las recibía. Un administrador no puede ni entrar a la app (el login lo rechaza), así que esas
-- opciones nunca le sirven.
--
--   1. `menu.mobile_only`: marca las opciones que son solo de la app móvil. `GET /menu/for-current-user` no se las
--      entrega a quien es propietario o administrador, tenga el rol que tenga; los demás roles las reciben como siempre.
--      A diferencia de ocultar la asignación de un rol (`roles_menu.visible`), esto no depende de qué roles tenga la
--      cuenta. La pantalla «Menús» (`GET /menu`) sigue mostrando el árbol completo: ahí se administran.
--   2. Se marcan «Inicio» (/home), «Perfil» (/profile) y el grupo «App móvil» que las contiene. No se desactivan ni se
--      borran: la app móvil las sigue usando (barra inferior, `nav_order`).
--
-- Se buscan por url y por nombre de grupo, nunca por id. Es idempotente: reaplicarla no cambia nada. Lo que cada rol
-- recibe en `roles_menu` no cambia.
-- =====================================================================================================================


-- 1. Opciones del menú que son solo de la app móvil
ALTER TABLE menu ADD COLUMN IF NOT EXISTS mobile_only BOOLEAN NOT NULL DEFAULT FALSE;


-- 2. Las opciones de la app móvil y su grupo
UPDATE menu
SET mobile_only = TRUE,
    updated_at = LOCALTIMESTAMP(0)
WHERE deleted_at IS NULL
  AND mobile_only = FALSE
  AND (url IN ('/home', '/profile') OR (name = 'App móvil' AND url IS NULL));
