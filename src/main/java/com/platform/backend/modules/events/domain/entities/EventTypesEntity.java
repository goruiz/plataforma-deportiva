package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import com.platform.backend.shared.domain.interfaces.HasUniqueField;
import jakarta.persistence.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "events_types")
public class EventTypesEntity extends BaseTranslatableEntity implements HasUniqueField {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @OneToMany(mappedBy = "eventType", fetch = FetchType.LAZY)
    private List<EventsEntity> events;

    public EventTypesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public List<EventsEntity> getEvents() { return events; }
    public void setEvents(List<EventsEntity> events) { this.events = events; }

    @Override
    public void releaseUniqueFields(UUID entityId) {
        if (this.name != null) {
            this.name = this.name + "_DELETED_" + entityId;
        }
    }
}
