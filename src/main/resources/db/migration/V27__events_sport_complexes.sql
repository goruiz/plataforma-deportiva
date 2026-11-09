-- =====================================================================================================================
-- V27 · Los complejos donde se juega un evento
--
-- Un evento se juega en uno o varios complejos deportivos. El calendario automático reparte sus partidos entre las canchas
-- de esos complejos (hasta ahora todos iban a una sola cancha, la de la configuración del calendario). La relación es una
-- tabla de enlace, como `events_categories`: borrado lógico, y un complejo solo puede estar una vez en un evento mientras
-- el enlace esté activo.
--
-- Los eventos que ya existen quedan sin complejos y su calendario se sigue generando como antes (con la cancha de su
-- configuración, si tienen una). No siembra datos ni menú: ninguna pantalla nueva.
--
-- Es idempotente: reaplicarla no cambia nada.
-- =====================================================================================================================


CREATE TABLE IF NOT EXISTS events_sport_complexes (
    id                UUID PRIMARY KEY,
    id_event          UUID NOT NULL,
    id_sport_complex  UUID NOT NULL,
    created_by        UUID,
    created_at        TIMESTAMP(6) NOT NULL,
    updated_by        UUID,
    updated_at        TIMESTAMP(6),
    deleted_by        UUID,
    deleted_at        TIMESTAMP(6),
    CONSTRAINT fk_events_sport_complexes_event FOREIGN KEY (id_event) REFERENCES events (id),
    CONSTRAINT fk_events_sport_complexes_complex FOREIGN KEY (id_sport_complex) REFERENCES sport_complexes (id)
);

-- Un complejo una sola vez por evento (los enlaces borrados no cuentan, así que se puede volver a agregar).
CREATE UNIQUE INDEX IF NOT EXISTS uq_events_sport_complexes_active
    ON events_sport_complexes (id_event, id_sport_complex)
    WHERE deleted_at IS NULL;

-- «¿Qué eventos usan este complejo?»
CREATE INDEX IF NOT EXISTS idx_events_sport_complexes_complex
    ON events_sport_complexes (id_sport_complex)
    WHERE deleted_at IS NULL;
