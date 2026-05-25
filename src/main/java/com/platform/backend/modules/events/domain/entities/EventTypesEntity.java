package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "events_types")
public class EventTypesEntity extends BaseTranslatableEntity {

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
}
