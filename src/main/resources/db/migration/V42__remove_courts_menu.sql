-- =====================================================================================================================
-- V42 · Se borra «Canchas» (/courts) del menú
--
-- La V26 la ocultó a los roles del panel y la V35 al rol «Usuario» (la app móvil), pero dejaron la fila: por eso seguía
-- apareciendo en las pantallas «Menús» y «Menú por rol» del propietario, que muestran el árbol completo. Ningún rol la usa
-- ya: las canchas se ven y se administran dentro de su complejo, en «Complejos y canchas» (/sport-complexes), tanto en el
-- panel como en la app. Así que la opción deja de existir para todos.
--
--   1. Se borra como lo hace `DELETE /menu/{id}`: borrado lógico, el nombre se libera y sus asignaciones de rol se quitan
--      (un rol no debe «ver» algo que ya no existe). Es lo mismo que hicieron la V19 con «Liga» y «Torneo» y la V24 con
--      «Ligas».
--   2. «Complejos deportivos» ya es la primera opción de su grupo (orden 0 desde la V9): no hay nada que renumerar.
--
-- Árbol resultante (lo demás no cambia):
--
--   Instalaciones   Complejos deportivos (en el panel y en la app se llama «Complejos y canchas»; el texto sale del
--                   diccionario)
--
-- Solo cambia el menú: la API de canchas (`/courts`, `/courts/{id}/availability`) sigue igual, porque es la que usan
-- «Complejos y canchas» y los eventos.
--
-- Se busca por url, nunca por id. Es idempotente: si ya no existe no toca nada, y una opción borrada antes con esa url
-- (su nombre ya lleva el sufijo `_DELETED_`) tampoco se toca.
-- =====================================================================================================================


-- Primero sus asignaciones (se buscan a través de la opción, que aún no está borrada) y luego la opción
UPDATE roles_menu assignment
SET deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
FROM menu item
WHERE assignment.id_menu = item.id
  AND item.url = '/courts'
  AND item.deleted_at IS NULL
  AND assignment.deleted_at IS NULL;

UPDATE menu
SET name = name || '_DELETED_' || id::text,
    deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
WHERE url = '/courts'
  AND deleted_at IS NULL;
