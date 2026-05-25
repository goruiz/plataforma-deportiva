package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

import java.util.List;

@Entity
@Table(name = "events_types")
public class EventTypesEntity extends BaseEntity {

    @Column(length = 100, nullable = false)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 255)
    private String translationKey;

    @OneToMany(mappedBy = "eventType", fetch = FetchType.LAZY)
    private List<EventsEntity> events;

    public EventTypesEntity() {}

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getTranslationKey() { return translationKey; }
    public void setTranslationKey(String translationKey) { this.translationKey = translationKey; }

    public List<EventsEntity> getEvents() { return events; }
    public void setEvents(List<EventsEntity> events) { this.events = events; }
}
