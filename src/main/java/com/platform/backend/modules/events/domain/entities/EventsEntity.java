package com.platform.backend.modules.events.domain.entities;

import com.platform.backend.shared.domain.entities.BaseTranslatableEntity;
import jakarta.persistence.*;

import java.time.LocalDate;

@Entity
@Table(name = "events")
public class EventsEntity extends BaseTranslatableEntity {

    @Column(length = 200)
    private String name;

    @Column(length = 255)
    private String description;

    @Column(length = 30)
    private String format;

    @Column(length = 20)
    private String status;

    @Column(name = "start_date")
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Column(name = "url_logo", length = 255)
    private String urlLogo;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_event_type", foreignKey = @ForeignKey(name = "fk_events_types_events"))
    private EventTypesEntity eventType;

    public EventsEntity() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFormat() {
        return format;
    }

    public void setFormat(String format) {
        this.format = format;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public EventTypesEntity getEventType() {
        return eventType;
    }

    public void setEventType(EventTypesEntity eventType) {
        this.eventType = eventType;
    }
}
