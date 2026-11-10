-- =====================================================================================================================
-- V35 · «Canchas» deja de ser una opción del menú de la app
--
-- En la app, «Complejos deportivos» (/sport-complexes) y «Canchas» (/courts) eran dos pantallas sobre lo mismo: las canchas
-- pertenecen a un complejo. Ahora son una sola, «Complejos y canchas» (/sport-complexes): cada complejo con sus canchas
-- dentro, y por defecto los complejos donde se juegan los eventos del equipo que el jugador tiene elegido. El texto de la
-- opción sale del diccionario de la app (`menu.admin.sport_complexes`); aquí solo se oculta la otra.
--
-- Igual que la V26 hizo con el panel, «Canchas» NO se borra: solo se oculta al rol «Usuario» (`visible = false`, sin borrar
-- la asignación), que es lo que hace la pantalla «Menú por rol» al quitarle una opción a un rol, así el propietario puede
-- devolverla desde allí. En la app, /courts sigue abriendo la misma pantalla unificada por si una cuenta aún trae el menú
-- anterior.
--
-- Árbol de la app resultante (lo demás no cambia):
--
--   Instalaciones   Complejos deportivos (en la app se llama «Complejos y canchas»; el texto sale del diccionario)
--
-- La opción se busca por url, nunca por id. Es idempotente: reaplicarla no cambia nada.
--
-- Placeholders de Flyway (se definen en application*.properties):
--   ${mobile_user_role_id}  UUID del rol «Usuario»; es el mismo app.security.mobile-user-role-id.
-- =====================================================================================================================


UPDATE roles_menu assignment
SET visible = false,
    updated_at = LOCALTIMESTAMP(0)
FROM menu item
WHERE assignment.id_menu = item.id
  AND item.url = '/courts'
  AND item.deleted_at IS NULL
  AND assignment.id_role = '${mobile_user_role_id}'::uuid
  AND assignment.deleted_at IS NULL
  AND assignment.visible IS DISTINCT FROM false;
