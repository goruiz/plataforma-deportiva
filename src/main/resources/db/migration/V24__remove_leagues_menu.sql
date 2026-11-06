-- =====================================================================================================================
-- V24 · Se borra «Ligas» (/ligas) del menú
--
-- La V23 la ocultó a los roles del panel y la dejó para el rol «Usuario» (la app móvil). Se decidió que las ligas no son
-- una opción aparte: son los eventos, y se ven en «Eventos» (en el panel, ordenados por quién los creó y por su tipo; en la
-- app móvil, en su pantalla «Eventos», que lista todo lo que el jugador ve). Así que la opción deja de existir para todos.
--
--   1. Se borra como lo hace `DELETE /menu/{id}`: borrado lógico, el nombre se libera y sus asignaciones de rol se quitan
--      (un rol no debe «ver» algo que ya no existe). Es lo mismo que hizo la V19 con «Liga» (/ranking) y «Torneo».
--   2. «Resultados y estadísticas» se queda con Partidos, Rankings y Reportes: se renumera su orden y se ajusta su
--      descripción, si nadie la cambió.
--
-- Árbol resultante:
--
--   Resultados y estadísticas   Partidos · Rankings · Reportes
--
-- Se busca por url, nunca por id. Es idempotente: si ya no existe no toca nada, y una opción borrada antes con esa url
-- (su nombre ya lleva el sufijo `_DELETED_`) tampoco se toca.
-- =====================================================================================================================


-- 1. Primero sus asignaciones (se buscan a través de la opción, que aún no está borrada) y luego la opción
UPDATE roles_menu assignment
SET deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
FROM menu item
WHERE assignment.id_menu = item.id
  AND item.url = '/ligas'
  AND item.deleted_at IS NULL
  AND assignment.deleted_at IS NULL;

UPDATE menu
SET name = name || '_DELETED_' || id::text,
    deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
WHERE url = '/ligas'
  AND deleted_at IS NULL;


-- 2. «Resultados y estadísticas» sin ella
UPDATE menu m
SET "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/matches',  0),
        ('/rankings', 1),
        ('/reports',  2)
) AS layout (url, item_order)
JOIN menu parent ON parent.name = 'Resultados y estadísticas' AND parent.url IS NULL AND parent.deleted_at IS NULL
WHERE m.url = layout.url
  AND m.id_parent_menu = parent.id
  AND m.deleted_at IS NULL
  AND m."order" IS DISTINCT FROM layout.item_order;

UPDATE menu
SET description = 'Partidos, rankings y reportes de solo lectura',
    updated_at = LOCALTIMESTAMP(0)
WHERE name = 'Resultados y estadísticas'
  AND url IS NULL
  AND deleted_at IS NULL
  AND description = 'Ligas, partidos, rankings y reportes de solo lectura';
