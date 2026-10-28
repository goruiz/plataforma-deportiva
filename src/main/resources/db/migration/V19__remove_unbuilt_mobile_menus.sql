-- =====================================================================================================================
-- V19 · Se quitan «Liga» (/ranking) y «Torneo» (/torneo) del menú
--
-- Las dos opciones venían de datos anteriores a las migraciones (ninguna las crea: la V14 solo las ordenó y se las dio al
-- rol «Usuario») y la app móvil nunca tuvo pantalla para ellas: abrirlas mostraba «Próximamente». Lo que prometían ya lo
-- dan pantallas reales: «Rankings» (/rankings) trae las posiciones de cada liga y «Ligas» (/ligas), los eventos.
--
--   1. Las dos se borran como lo hace `DELETE /menu/{id}`: borrado lógico, el nombre se libera y sus asignaciones de rol
--      se quitan (un rol no debe «ver» algo que ya no existe).
--   2. «App móvil» se queda con Inicio y Perfil: se renumera su orden y se ajusta su descripción, si nadie la cambió.
--
-- Se buscan por url, nunca por id. Es idempotente: si ya no existen no toca nada, y en una base creada desde cero tampoco
-- existen. Lo que ven los roles del panel (`roles_menu`) no cambia.
-- =====================================================================================================================


-- 1. Primero sus asignaciones (se buscan a través de la opción, que aún no está borrada) y luego las opciones
UPDATE roles_menu assignment
SET deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
FROM menu item
WHERE assignment.id_menu = item.id
  AND item.url IN ('/ranking', '/torneo')
  AND item.deleted_at IS NULL
  AND assignment.deleted_at IS NULL;

UPDATE menu
SET name = name || '_DELETED_' || id::text,
    deleted_at = LOCALTIMESTAMP(0),
    updated_at = LOCALTIMESTAMP(0)
WHERE url IN ('/ranking', '/torneo')
  AND deleted_at IS NULL;


-- 2. «App móvil» sin ellas
UPDATE menu m
SET "order" = layout.item_order,
    updated_at = LOCALTIMESTAMP(0)
FROM (VALUES
        ('/home',    0),
        ('/profile', 1)
) AS layout (url, item_order)
JOIN menu parent ON parent.name = 'App móvil' AND parent.url IS NULL AND parent.deleted_at IS NULL
WHERE m.url = layout.url
  AND m.id_parent_menu = parent.id
  AND m.deleted_at IS NULL
  AND m."order" IS DISTINCT FROM layout.item_order;

UPDATE menu
SET description = 'Lo que ve solo el rol Usuario en la app móvil: inicio y perfil',
    updated_at = LOCALTIMESTAMP(0)
WHERE name = 'App móvil'
  AND url IS NULL
  AND deleted_at IS NULL
  AND description = 'Lo que ve solo el rol Usuario en la app móvil: inicio, ligas, torneos y perfil';
