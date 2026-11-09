-- =====================================================================================================================
-- V26 · «Canchas» deja de ser una opción del menú del panel
--
-- En el panel, las canchas se administran dentro de su complejo: la pantalla «Complejos y canchas» (/sport-complexes) se
-- recorre de afuera hacia adentro (de quién es el complejo, cuál complejo y las canchas de ese complejo, donde se agregan,
-- editan y quitan), igual que «Eventos». «Canchas» (/courts) era una lista aparte de las mismas canchas, así que se quita
-- del menú del panel y todo se hace desde «Complejos y canchas». En el panel, /courts redirige a /sport-complexes.
--
-- La opción NO se borra: la app móvil la usa (rol «Usuario», V11 y V13) para que el jugador vea las canchas. Solo se oculta
-- a los dos roles del panel, Administrador y Propietario, igual que lo hace la pantalla «Menú por rol» al quitarle una
-- opción a un rol (`visible = false`, sin borrar la asignación): así el propietario puede devolverla desde esa pantalla.
--
-- Árbol del panel resultante (lo demás no cambia):
--
--   Instalaciones   Complejos deportivos (en el panel se llama «Complejos y canchas»; el texto sale del diccionario)
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
  AND item.url = '/courts'
  AND item.deleted_at IS NULL
  AND assignment.id_role IN ('${admin_role_id}'::uuid, '${owner_role_id}'::uuid)
  AND assignment.deleted_at IS NULL
  AND assignment.visible IS DISTINCT FROM false;
