package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseEntity;
import jakarta.persistence.*;

@Entity
@Table(name = "events_categories")
public class EventsCategoriesEntity extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_event", foreignKey = @ForeignKey(name = "fk_events_events_categories_2"))
    private EventsEntity event;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_category", foreignKey = @ForeignKey(name = "fk_categories_events_categories_1"))
    private CategoriesEntity category;

    public EventsCategoriesEntity() {}

    public EventsEntity getEvent() { return event; }
    public void setEvent(EventsEntity event) { this.event = event; }

    public CategoriesEntity getCategory() { return category; }
    public void setCategory(CategoriesEntity category) { this.category = category; }
}
