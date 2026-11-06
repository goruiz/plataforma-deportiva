-- =====================================================================================================================
-- V23 · «Ligas» deja de ser una opción del menú del panel
--
-- En el panel, las ligas son los eventos: la pantalla «Eventos» (/events) ya las muestra, ahora ordenadas por quién las
-- creó y por su tipo, con su avance y los resultados por registrar. «Ligas» (/ligas) era una copia de solo lectura de esa
-- misma lista, así que se quita del menú del panel y todo se hace desde «Eventos».
--
-- La opción NO se borra: la app móvil la usa (rol «Usuario», V10 y V13) para listar las ligas del jugador. Solo se oculta
-- a los dos roles del panel, Administrador y Propietario, igual que lo hace la pantalla «Menú por rol» al quitarle una
-- opción a un rol (`visible = false`, sin borrar la asignación): así el propietario puede devolverla desde esa pantalla.
--
-- Árbol del panel resultante (lo demás no cambia):
--
--   Resultados y estadísticas   Partidos · Rankings · Reportes
--
-- La opción se busca por url, nunca por id. Es idempotente: reaplicarla no cambia nada.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${admin_role_id}  UUID del rol Administrador; es el mismo app.security.admin-role-id.
--   ${owner_role_id}  UUID del rol Propietario; es el mismo app.security.owner-role-id.
-- =====================================================================================================================


UPDATE roles_menu assignment
SET visible = false,
    updated_at = LOCALTIMESTAMP(0)
FROM menu item
WHERE assignment.id_menu = item.id
  AND item.url = '/ligas'
  AND item.deleted_at IS NULL
  AND assignment.id_role IN ('${admin_role_id}'::uuid, '${owner_role_id}'::uuid)
  AND assignment.deleted_at IS NULL
  AND assignment.visible IS DISTINCT FROM false;
