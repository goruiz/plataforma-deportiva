package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import com.platform.backend.shared.domain.tenancy.TenantFilters;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import org.hibernate.annotations.Filter;

import java.util.UUID;

/**
 * A complex where an event is played. The calendar generator takes the courts of the event's complexes and spreads its
 * matches over them. Both ids are plain columns (like a match's court): the link is read in bulk and never navigated.
 */
@Entity
@Table(name = "events_sport_complexes")
@Filter(name = TenantFilters.NAME, condition = TenantFilters.OF_OWN_EVENT_BY_ID_EVENT, deduceAliasInjectionPoints = false)
public class EventsSportComplexesEntity extends BaseEntity {

    @Column(name = "id_event", nullable = false)
    private UUID eventId;

    @Column(name = "id_sport_complex", nullable = false)
    private UUID sportComplexId;

    public EventsSportComplexesEntity() {}

    public EventsSportComplexesEntity(UUID eventId, UUID sportComplexId) {
        this.eventId = eventId;
        this.sportComplexId = sportComplexId;
    }

    public UUID getEventId() { return eventId; }
    public void setEventId(UUID eventId) { this.eventId = eventId; }

    public UUID getSportComplexId() { return sportComplexId; }
    public void setSportComplexId(UUID sportComplexId) { this.sportComplexId = sportComplexId; }
}
