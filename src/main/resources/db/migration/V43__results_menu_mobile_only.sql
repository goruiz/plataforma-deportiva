-- =====================================================================================================================
-- V43 · «Resultados y estadísticas» (Partidos, Rankings y Reportes) pasa a ser solo de la app móvil
--
-- En el panel ya no son pantallas aparte: lo que mostraban se ve dentro de cada evento, en «Eventos» (los partidos del
-- evento, sus posiciones y líderes y su reporte), así que el grupo sale del menú lateral del propietario y de los
-- administradores. La app móvil, en cambio, sigue usando las tres opciones (rol «Usuario»: V15), por eso no se borran
-- ni se desactivan: se marcan como `mobile_only`, que es lo que ya hizo la V41 con «App móvil», «Inicio» y «Perfil».
--
--   · `GET /menu/for-current-user` no entrega las opciones `mobile_only` a quien es propietario o administrador, tenga el
--     rol que tenga, y un grupo se oculta solo cuando nada de lo que tiene dentro es visible (`MenuTree.visibleFor`).
--   · Lo que cada rol recibe en `roles_menu` no cambia: el rol «Usuario» sigue viendo las tres opciones en la app.
--   · La pantalla «Menús» (`GET /menu`) sigue mostrando el árbol completo: ahí se administran.
--
-- Árbol resultante del panel (lo demás no cambia): «Resultados y estadísticas» ya no aparece.
--
-- Se buscan por url y por nombre de grupo, nunca por id. Es idempotente: reaplicarla no cambia nada.
-- Requiere la columna `menu.mobile_only` que crea la V41.
-- =====================================================================================================================


UPDATE menu
SET mobile_only = TRUE,
    updated_at = LOCALTIMESTAMP(0)
WHERE deleted_at IS NULL
  AND mobile_only = FALSE
  AND (url IN ('/matches', '/rankings', '/reports') OR (name = 'Resultados y estadísticas' AND url IS NULL));
